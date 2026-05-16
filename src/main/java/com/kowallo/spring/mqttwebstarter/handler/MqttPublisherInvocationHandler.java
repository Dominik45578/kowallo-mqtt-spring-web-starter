package com.kowallo.spring.mqttwebstarter.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kowallo.spring.mqttwebstarter.annotation.MqttPayload;
import com.kowallo.spring.mqttwebstarter.annotation.MqttPublisher;
import com.kowallo.spring.mqttwebstarter.annotation.MqttPublisherTopic;
import com.kowallo.spring.mqttwebstarter.annotation.TopicVariable;
import com.kowallo.spring.mqttwebstarter.exceptions.MqttFrameworkException;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class MqttPublisherInvocationHandler implements InvocationHandler {

    private final MessageChannel mqttOutputChannel;
    private final ObjectMapper objectMapper;
    private final String classTopicPrefix;

    public MqttPublisherInvocationHandler(MessageChannel mqttOutputChannel, ObjectMapper objectMapper, Class<?> targetInterface) {
        this.mqttOutputChannel = mqttOutputChannel;
        this.objectMapper = objectMapper;
        
        MqttPublisher annotation = targetInterface.getAnnotation(MqttPublisher.class);
        this.classTopicPrefix = (annotation != null) ? annotation.value() : "";
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        MqttPublisherTopic publisherTopic = method.getAnnotation(MqttPublisherTopic.class);
        if (publisherTopic == null) {
            throw new MqttFrameworkException("Method " + method.getName() + " is missing @MqttPublisherTopic annotation");
        }

        String topicTemplate = classTopicPrefix + publisherTopic.value();

        Map<String, String> variables = new HashMap<>();
        Object payloadData = null;

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Object argValue = (args != null) ? args[i] : null;

            if (param.isAnnotationPresent(TopicVariable.class)) {
                TopicVariable varAnno = param.getAnnotation(TopicVariable.class);
                String varName = varAnno.value().isEmpty() ? param.getName() : varAnno.value();
                variables.put(varName, argValue != null ? argValue.toString() : "");
            } else if (param.isAnnotationPresent(MqttPayload.class) || payloadData == null) {
                payloadData = argValue;
            }
        }

        String finalTopic = resolveTopic(topicTemplate, variables);

        byte[] payloadBytes;
        if (payloadData instanceof byte[]) {
            payloadBytes = (byte[]) payloadData;
        } else if (payloadData instanceof String) {
            payloadBytes = ((String) payloadData).getBytes();
        } else if (payloadData != null) {
            payloadBytes = objectMapper.writeValueAsBytes(payloadData);
        } else {
            payloadBytes = new byte[0];
        }

        MessageBuilder<byte[]> messageBuilder = MessageBuilder.withPayload(payloadBytes)
                .setHeader(MqttHeaders.TOPIC, finalTopic);

        if (publisherTopic.qos() >= 0) {
            messageBuilder.setHeader(MqttHeaders.QOS, publisherTopic.qos());
        }

        Message<byte[]> message = messageBuilder.build();

        mqttOutputChannel.send(message);

        return null;
    }

    private String resolveTopic(String template, Map<String, String> variables) {
        String resolved = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        if (resolved.contains("{") && resolved.contains("}")) {
            throw new MqttFrameworkException("Not all topic variables were resolved for template: " + template);
        }
        return resolved;
    }
}