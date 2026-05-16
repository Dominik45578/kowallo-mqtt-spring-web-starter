package com.kowallo.spring.mqttwebstarter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kowallo.spring.mqttwebstarter.handler.MqttEndpointRegistry;
import com.kowallo.spring.mqttwebstarter.handler.MqttMessageDispatcher;
import com.kowallo.spring.mqttwebstarter.handler.MqttPublisherRegistrar;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

@AutoConfiguration
@ConditionalOnClass(MqttClient.class)
@ConditionalOnProperty(prefix = "spring.mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MqttProperties.class)
@Import(MqttPublisherRegistrar.class)
public class MqttAutoConfiguration {

    private static final long DEFAULT_COMPLETION_TIMEOUT = 5000L;
    private static final String INBOUND_CLIENT_ID_SUFFIX = "-inbound";
    private static final String OUTBOUND_CLIENT_ID_SUFFIX = "-outbound";

    @Bean
    @ConditionalOnMissingBean
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
    @ConditionalOnMissingBean
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
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter inboundAdapter(
            MqttPahoClientFactory clientFactory,
            MqttProperties properties,
            MessageChannel mqttInputChannel) {

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.getClientId() + INBOUND_CLIENT_ID_SUFFIX,
                clientFactory,
                properties.getTopics()
        );

        adapter.setCompletionTimeout(DEFAULT_COMPLETION_TIMEOUT);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(properties.getDefaultQos());
        adapter.setOutputChannel(mqttInputChannel);

        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler outboundGateway(MqttPahoClientFactory clientFactory, MqttProperties properties) {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                properties.getClientId() + OUTBOUND_CLIENT_ID_SUFFIX,
                clientFactory
        );

        messageHandler.setAsync(true);
        messageHandler.setCompletionTimeout(DEFAULT_COMPLETION_TIMEOUT);
        messageHandler.setDefaultQos(properties.getDefaultQos());
        messageHandler.setDefaultTopic(properties.getDefaultTopic());

        return messageHandler;
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