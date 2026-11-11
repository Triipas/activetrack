package com.activetrack.notificaciones.messaging;

import com.activetrack.events.Colas;
import com.activetrack.events.ResultadoActualizado;
import com.activetrack.notificaciones.observer.CruceDTO;
import com.activetrack.notificaciones.service.GestorSeguimientos;
import com.activetrack.notificaciones.service.LeaderboardBroadcaster;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Patrón Observer — rol <b>Client</b>.
 * Recibe ResultadoActualizado desde RabbitMQ y registra el cruce en el AtletaSeguido,
 * que se encarga de notificar a todos sus seguidores.
 */
@Component
public class ResultadoEventListener {

    private final GestorSeguimientos gestor;
    private final LeaderboardBroadcaster broadcaster;

    public ResultadoEventListener(GestorSeguimientos gestor, LeaderboardBroadcaster broadcaster) {
        this.gestor = gestor;
        this.broadcaster = broadcaster;
    }

    @RabbitListener(queues = Colas.COLA_NOTIFICACIONES_RESULTADOS)
    public void onResultadoActualizado(ResultadoActualizado evento) {
        gestor.registrarCruce(CruceDTO.desde(evento));
        broadcaster.notificarCambio(evento.eventoId(), evento.inscripcionId(), evento.recibidoEn());
    }
}
