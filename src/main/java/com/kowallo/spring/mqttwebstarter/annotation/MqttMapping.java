package com.kowallo.spring.mqttwebstarter.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttMapping {
    /**
     * Wzorzec topicu, np. "user/{id}/temp" lub "device/+/status"
     */
    String value();
}