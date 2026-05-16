package com.kowallo.spring.mqttwebstarter.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqttPublisherTopic {
    /**
     * Temat MQTT, np. "spring/{id}/command". Może zawierać zmienne.
     */
    String value();

    /**
     * Jakość obsługi (QoS) dla tej konkretnej publikacji. 
     * Jeśli ujemna, framework użyje domyślnego QoS z konfiguracji.
     */
    int qos() default -1;
}