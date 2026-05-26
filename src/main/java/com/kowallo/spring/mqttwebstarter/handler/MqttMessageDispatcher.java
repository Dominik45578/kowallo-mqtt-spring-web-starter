package com.kowallo.spring.mqttwebstarter.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kowallo.spring.mqttwebstarter.annotation.MqttHeader;
import com.kowallo.spring.mqttwebstarter.annotation.MqttPayload;
import com.kowallo.spring.mqttwebstarter.annotation.TopicVariable;
import com.kowallo.spring.mqttwebstarter.exceptions.MqttFrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.AntPathMatcher;

import java.lang.reflect.Parameter;
import java.util.Map;

public class MqttMessageDispatcher implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageDispatcher.class);

    private final MqttEndpointRegistry registry;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher;

    public MqttMessageDispatcher(MqttEndpointRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        if (topic == null) {
            log.error("Odrzucono wiadomość MQTT: brak nagłówka z tematem (RECEIVED_TOPIC jest null)");
            return;
        }

        MqttEndpointRegistry.MqttHandlerMethod handler = findHandler(topic);
        if (handler == null) {
            // Zamiast rzucać wyjątek i zrywać połączenie, logujemy informację i konsumujemy wiadomość.
            // Broker otrzyma PUBACK i usunie wiadomość (np. zaległe lockly/device/...) z kolejki.
            log.warn("Odrzucono nieobsługiwany temat MQTT (brak zarejestrowanego kontrolera): {}", topic);
            return;
        }

        try {
            Object[] args = resolveArguments(handler, message, topic);
            handler.method().setAccessible(true);
            handler.method().invoke(handler.bean(), args);
            log.debug("Pomyślnie obsłużono punkt końcowy MQTT dla tematu: {}", topic);
        } catch (Exception e) {
            // Logujemy błąd biznesowy/refleksji, ale nie pozwalamy na ubicie głównego callbacku Paho
            log.error("Wystąpił błąd podczas wykonywania metody kontrolera MQTT dla tematu: {}", topic, e);
        }
    }

    private MqttEndpointRegistry.MqttHandlerMethod findHandler(String actualTopic) {
        for (MqttEndpointRegistry.MqttHandlerMethod handler : registry.getHandlers()) {
            if (pathMatcher.match(handler.topicPattern(), actualTopic)) {
                return handler;
            }
        }
        return null;
    }

    private Object[] resolveArguments(MqttEndpointRegistry.MqttHandlerMethod handler, Message<?> message, String actualTopic) throws Exception {
        Parameter[] parameters = handler.method().getParameters();
        Object[] args = new Object[parameters.length];

        Map<String, String> templateVariables = pathMatcher.extractUriTemplateVariables(handler.topicPattern(), actualTopic);

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            if (parameter.isAnnotationPresent(TopicVariable.class)) {
                String varName = parameter.getAnnotation(TopicVariable.class).value();
                String value = templateVariables.get(varName);
                args[i] = convertSimpleType(value, parameter.getType());
            }
            else if (parameter.isAnnotationPresent(MqttPayload.class)) {
                args[i] = resolvePayload(message.getPayload(), parameter.getType());
            }
            else if (parameter.isAnnotationPresent(MqttHeader.class)) {
                String headerName = parameter.getAnnotation(MqttHeader.class).value();
                args[i] = message.getHeaders().get(headerName, parameter.getType());
            }
            else if (Message.class.isAssignableFrom(parameter.getType())) {
                args[i] = message;
            }
            else {
                args[i] = null;
            }
        }
        return args;
    }

    private Object resolvePayload(Object rawPayload, Class<?> targetType) throws Exception {
        if (targetType.isInstance(rawPayload)) {
            return rawPayload;
        }
        if (rawPayload instanceof String jsonString) {
            return objectMapper.readValue(jsonString, targetType);
        }
        if (rawPayload instanceof byte[] bytes) {
            return objectMapper.readValue(bytes, targetType);
        }
        throw new MqttFrameworkException("Konwersja payloadu nieobsługiwana dla typu: " + rawPayload.getClass());
    }

    private Object convertSimpleType(String value, Class<?> targetType) {
        if (targetType == String.class) return value;
        if (targetType == Long.class || targetType == long.class) return Long.parseLong(value);
        if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value);
        throw new MqttFrameworkException("Typ zmiennej tematu (TopicVariable) nie jest obsługiwany: " + targetType);
    }
}