package com.kowallo.spring.mqttwebstarter.exceptions;

public class MqttFrameworkException extends RuntimeException {
    public MqttFrameworkException(String message) {
        super(message);
    }
    public MqttFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}

