package com.petvet.citas.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
    public static final String EXCHANGE = "petvet.events";

    @Bean
    public TopicExchange petvetEvents() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
