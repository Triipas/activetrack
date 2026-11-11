package com.activetrack.resultados.config;

import com.activetrack.events.Colas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class MensajeriaConfig {

    private static final String DLQ = Colas.COLA_RESULTADOS_CRUCES + ".dlq";

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(Colas.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(Colas.DLX, true, false);
    }

    /** Cola durable: si este servicio se cae, los cruces esperan aquí y no se pierden. */
    @Bean
    public Queue colaCruces() {
        return QueueBuilder.durable(Colas.COLA_RESULTADOS_CRUCES)
                .withArgument("x-dead-letter-exchange", Colas.DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue colaCrucesFallidos() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding bindingCruces(Queue colaCruces, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaCruces).to(eventosExchange).with(Colas.RK_CRUCE);
    }

    @Bean
    public Binding bindingCrucesFallidos(Queue colaCrucesFallidos, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(colaCrucesFallidos).to(deadLetterExchange).with(DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
