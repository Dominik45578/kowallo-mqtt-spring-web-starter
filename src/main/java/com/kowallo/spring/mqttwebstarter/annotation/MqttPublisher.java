package com.kowallo.spring.mqttwebstarter.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttPublisher {
    /**
     * Opcjonalny prefiks tematu dla wszystkich metod w danym interfejsie.
     */
    String value() default "";
}