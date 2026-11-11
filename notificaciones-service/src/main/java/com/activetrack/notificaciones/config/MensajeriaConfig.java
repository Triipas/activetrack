package com.activetrack.notificaciones.config;

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

@Configuration
public class MensajeriaConfig {

    private static final String DLQ = Colas.COLA_NOTIFICACIONES_RESULTADOS + ".dlq";

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(Colas.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(Colas.DLX, true, false);
    }

    @Bean
    public Queue colaResultados() {
        return QueueBuilder.durable(Colas.COLA_NOTIFICACIONES_RESULTADOS)
                .withArgument("x-dead-letter-exchange", Colas.DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Queue colaResultadosFallidos() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding bindingResultados(Queue colaResultados, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaResultados).to(eventosExchange).with(Colas.RK_RESULTADO);
    }

    @Bean
    public Binding bindingResultadosFallidos(Queue colaResultadosFallidos, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(colaResultadosFallidos).to(deadLetterExchange).with(DLQ);
    }

    @Bean
    public Queue colaInscripciones() {
        return QueueBuilder.durable(Colas.COLA_NOTIFICACIONES_INSCRIPCIONES)
                .withArgument("x-dead-letter-exchange", Colas.DLX)
                .withArgument("x-dead-letter-routing-key", Colas.COLA_NOTIFICACIONES_INSCRIPCIONES + ".dlq")
                .build();
    }

    @Bean
    public Queue colaInscripcionesFallidas() {
        return QueueBuilder.durable(Colas.COLA_NOTIFICACIONES_INSCRIPCIONES + ".dlq").build();
    }

    @Bean
    public Binding bindingInscripciones(Queue colaInscripciones, TopicExchange eventosExchange) {
        return BindingBuilder.bind(colaInscripciones).to(eventosExchange).with(Colas.RK_INSCRIPCIONES);
    }

    @Bean
    public Binding bindingInscripcionesFallidas(Queue colaInscripcionesFallidas, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(colaInscripcionesFallidas).to(deadLetterExchange)
                .with(Colas.COLA_NOTIFICACIONES_INSCRIPCIONES + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
