package com.kowallo.spring.mqttwebstarter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kowallo.spring.mqttwebstarter.MqttEndpointRegistry;
import com.kowallo.spring.mqttwebstarter.MqttMessageDispatcher;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

@AutoConfiguration
@ConditionalOnClass(MqttClient.class)
@ConditionalOnProperty(prefix = "spring.mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MqttProperties.class)
public class MqttAutoConfiguration {

    @Bean
    public MqttConnectOptions mqttConnectOptions(MqttProperties properties) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getUrl()});
        options.setCleanSession(properties.isCleanSession());
        options.setConnectionTimeout(properties.getConnectionTimeout());
        options.setKeepAliveInterval(properties.getKeepAliveInterval());
        options.setAutomaticReconnect(properties.isAutomaticReconnect());
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        if (StringUtils.hasText(properties.getUsername())) {
            options.setUserName(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            options.setPassword(properties.getPassword().toCharArray());
        }

        return options;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttConnectOptions options) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter inboundAdapter(
            MqttPahoClientFactory clientFactory,
            MqttProperties properties,
            MessageChannel mqttInputChannel) {

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.getClientId() + "-inbound",
                clientFactory,
                properties.getTopics()
        );

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(properties.getDefaultQos());
        adapter.setOutputChannel(mqttInputChannel);

        return adapter;
    }


    @Bean
    @ConditionalOnMissingBean
    public MqttEndpointRegistry mqttEndpointRegistry(ApplicationContext applicationContext) {
        return new MqttEndpointRegistry(applicationContext);
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttMessageDispatcher(MqttEndpointRegistry registry, ObjectMapper objectMapper) {
        return new MqttMessageDispatcher(registry, objectMapper);
    }

}