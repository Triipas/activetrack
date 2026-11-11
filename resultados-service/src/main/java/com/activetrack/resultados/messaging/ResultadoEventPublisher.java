package com.activetrack.resultados.messaging;

import com.activetrack.events.Colas;
import com.activetrack.events.ResultadoActualizado;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publica ResultadoActualizado para que Notificaciones avise a los espectadores. */
@Component
public class ResultadoEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ResultadoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(ResultadoActualizado evento) {
        rabbitTemplate.convertAndSend(Colas.EXCHANGE, Colas.RK_RESULTADO, evento);
    }
}
