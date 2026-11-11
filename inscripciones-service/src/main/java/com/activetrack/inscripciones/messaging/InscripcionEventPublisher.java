package com.activetrack.inscripciones.messaging;

import com.activetrack.events.Colas;
import com.activetrack.events.InscripcionConfirmada;
import com.activetrack.events.InscripcionReservada;
import com.activetrack.inscripciones.service.Transacciones;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publica los eventos de inscripción recién cuando la transacción se confirma. */
@Component
public class InscripcionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public InscripcionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void reservada(InscripcionReservada evento) {
        Transacciones.alConfirmar(() ->
                rabbitTemplate.convertAndSend(Colas.EXCHANGE, Colas.RK_INSCRIPCION_RESERVADA, evento));
    }

    public void confirmada(InscripcionConfirmada evento) {
        Transacciones.alConfirmar(() ->
                rabbitTemplate.convertAndSend(Colas.EXCHANGE, Colas.RK_INSCRIPCION_CONFIRMADA, evento));
    }
}
