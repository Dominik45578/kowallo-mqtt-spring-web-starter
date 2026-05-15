package com.kowallo.spring.mqttwebstarter.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Validated
@ConfigurationProperties(prefix = "spring.mqtt")
public class MqttProperties {

    /**
     * Włącza lub wyłącza auto-konfigurację MQTT.
     */
    private boolean enabled = true;

    /**
     * Adres hosta brokera MQTT (np. tcp://localhost:1883).
     */
    @NotBlank(message = "Adres URL brokera MQTT (spring.mqtt.url) nie może być pusty")
    private String url;

    /**
     * Unikalny identyfikator klienta. Jeśli nie podano, generowany jest losowy UUID.
     */
    private String clientId = "spring-mqtt-" + UUID.randomUUID().toString().substring(0, 8);

    /**
     * Globalne topici do nasłuchiwania w momencie startu aplikacji.
     */
    @NotEmpty(message = "Musisz zdefiniować przynajmniej jeden topic do nasłuchiwania")
    private String[] topics = {"#"};

    /**
     * Quality of Service (0 - At most once, 1 - At least once, 2 - Exactly once).
     */
    @Min(0) @Max(2)
    private int defaultQos = 1;

    private String username;
    private String password;
    private boolean cleanSession = true;
    private int connectionTimeout = 10;
    private int keepAliveInterval = 60;
    private boolean automaticReconnect = true;
    private String version = "3.1.1";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String[] getTopics() { return topics; }
    public void setTopics(String[] topics) { this.topics = topics; }

    public int getDefaultQos() { return defaultQos; }
    public void setDefaultQos(int defaultQos) { this.defaultQos = defaultQos; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isCleanSession() { return cleanSession; }
    public void setCleanSession(boolean cleanSession) { this.cleanSession = cleanSession; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public int getKeepAliveInterval() { return keepAliveInterval; }
    public void setKeepAliveInterval(int keepAliveInterval) { this.keepAliveInterval = keepAliveInterval; }

    public boolean isAutomaticReconnect() { return automaticReconnect; }
    public void setAutomaticReconnect(boolean automaticReconnect) { this.automaticReconnect = automaticReconnect; }
}