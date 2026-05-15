package com.kowallo.spring.mqttwebstarter.annotation;
import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttPayload {
    boolean required() default true;
}