package com.kowallo.spring.mqttwebstarter.handler;

import com.kowallo.spring.mqttwebstarter.annotation.MqttController;
import com.kowallo.spring.mqttwebstarter.annotation.MqttMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MqttEndpointRegistry implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(MqttEndpointRegistry.class);
    private final ApplicationContext applicationContext;
    private final List<MqttHandlerMethod> handlers = new ArrayList<>();

    public MqttEndpointRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(MqttController.class);
        
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Method[] methods = bean.getClass().getDeclaredMethods();
            
            for (Method method : methods) {
                if (method.isAnnotationPresent(MqttMapping.class)) {
                    MqttMapping mapping = method.getAnnotation(MqttMapping.class);
                    handlers.add(new MqttHandlerMethod(mapping.value(), bean, method));
                    log.info("Registered new mqtt endpoint: [{}] -> {}.{}",
                            mapping.value(), bean.getClass().getSimpleName(), method.getName());
                }
            }
        }
    }

    public List<MqttHandlerMethod> getHandlers() {
        return this.handlers;
    }

    public record MqttHandlerMethod(String topicPattern, Object bean, Method method) {}
}