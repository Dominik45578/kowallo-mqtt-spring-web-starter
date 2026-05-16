package com.kowallo.spring.mqttwebstarter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kowallo.spring.mqttwebstarter.handler.MqttPublisherInvocationHandler;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;

import java.lang.reflect.Proxy;

public class MqttPublisherFactoryBean implements FactoryBean<Object> {

    private final Class<?> interfaceType;

    @Autowired
    private MessageChannel mqttOutputChannel;

    @Autowired
    private ObjectMapper objectMapper;

    public MqttPublisherFactoryBean(Class<?> interfaceType) {
        this.interfaceType = interfaceType;
    }

    @Override
    public Object getObject() throws Exception {
        return Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                new MqttPublisherInvocationHandler(mqttOutputChannel, objectMapper, interfaceType)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return this.interfaceType;
    }
}