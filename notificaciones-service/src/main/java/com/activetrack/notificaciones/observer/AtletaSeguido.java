package com.activetrack.notificaciones.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Patrón Observer — rol <b>Subject</b>.
 * Representa a un atleta que es seguido por espectadores. Cuando registra un cruce,
 * notifica a todos sus seguidores sin saber si son WebSocket, push u otro canal.
 */
public class AtletaSeguido {

    private static final Logger log = LoggerFactory.getLogger(AtletaSeguido.class);

    private final Long atletaId;
    private final List<SeguidorAtleta> seguidores = new CopyOnWriteArrayList<>();

    public AtletaSeguido(Long atletaId) {
        this.atletaId = atletaId;
    }

    public void suscribir(SeguidorAtleta seguidor) {
        desuscribir(seguidor.id());
        seguidores.add(seguidor);
    }

    public void desuscribir(String seguidorId) {
        seguidores.removeIf(s -> s.id().equals(seguidorId));
    }

    public void registrarCruce(CruceDTO cruce) {
        notificar(cruce);
    }

    private void notificar(CruceDTO cruce) {
        seguidores.removeIf(s -> !s.activo());
        for (SeguidorAtleta seguidor : seguidores) {
            try {
                seguidor.actualizar(cruce);
            } catch (RuntimeException e) {
                // Un canal que falla no debe impedir que los demás reciban la notificación.
                log.warn("No se pudo notificar al seguidor {}: {}", seguidor.id(), e.getMessage());
            }
        }
    }

    public Long getAtletaId() {
        return atletaId;
    }

    public int cantidadSeguidores() {
        return seguidores.size();
    }
}
