package com.kowallo.spring.mqttwebstarter.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttHeader {
    /**
     * Header keys - not supported using mqttv3x
     */
    String value();
}