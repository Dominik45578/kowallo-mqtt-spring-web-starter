package com.kowallo.spring.mqttwebstarter.exceptions;

public class MqttNoHandlerFoundException extends MqttFrameworkException {
    public MqttNoHandlerFoundException(String topic) {
        super(String.format("Nie znaleziono kontrolera MQTT dla topicu: %s", topic));
    }
}