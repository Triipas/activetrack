package com.activetrack.cronometraje.messaging;

import com.activetrack.events.AtletaCruzoPuntoControl;
import com.activetrack.events.Colas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publica el evento AtletaCruzoPuntoControl en el broker. */
@Component
public class CruceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CruceEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public CruceEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(AtletaCruzoPuntoControl evento) {
        rabbitTemplate.convertAndSend(Colas.EXCHANGE, Colas.RK_CRUCE, evento);
        log.info("Publicado AtletaCruzoPuntoControl inscripcion={} punto={}",
                evento.inscripcionId(), evento.puntoControlId());
    }
}
