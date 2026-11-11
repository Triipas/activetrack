package com.activetrack.resultados.messaging;

import com.activetrack.events.AtletaCruzoPuntoControl;
import com.activetrack.events.Colas;
import com.activetrack.resultados.service.CalculoTiemposService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/** Consume AtletaCruzoPuntoControl desde RabbitMQ. */
@Component
public class CruceEventListener {

    private static final Logger log = LoggerFactory.getLogger(CruceEventListener.class);

    private final CalculoTiemposService calculoTiempos;

    public CruceEventListener(CalculoTiemposService calculoTiempos) {
        this.calculoTiempos = calculoTiempos;
    }

    @RabbitListener(queues = Colas.COLA_RESULTADOS_CRUCES)
    public void onCruce(AtletaCruzoPuntoControl cruce) {
        try {
            calculoTiempos.procesar(cruce);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            // Dato inválido: reintentar no lo arregla, se envía directo a la cola de errores.
            log.error("Cruce inválido descartado a la DLQ: {}", e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}
