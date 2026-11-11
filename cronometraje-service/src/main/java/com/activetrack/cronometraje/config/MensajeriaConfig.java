package com.activetrack.cronometraje.config;

import com.activetrack.events.Colas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MensajeriaConfig {

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(Colas.EXCHANGE, true, false);
    }

    /** Los eventos viajan como JSON. */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
