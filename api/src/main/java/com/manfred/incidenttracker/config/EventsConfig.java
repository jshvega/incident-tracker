package com.manfred.incidenttracker.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Configuration
@ConditionalOnProperty(name = "events.enabled", havingValue = "true")
public class EventsConfig {

    @Bean
    EventBridgeClient eventBridgeClient() {
        return EventBridgeClient.builder()
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();
    }
}