package com.activetrack.notificaciones.observer;

import com.activetrack.events.ResultadoActualizado;

import java.time.Instant;

/** Información que reciben los seguidores cada vez que el atleta cruza un punto. */
public record CruceDTO(
        Long eventoId,
        Long atletaId,
        Long inscripcionId,
        String nombre,
        int dorsal,
        String categoria,
        String puntoControl,
        double km,
        boolean esMeta,
        long tiempoNetoMs,
        Long posicionGeneral,
        Long posicionCategoria,
        String estado,
        Instant recibidoEn) {

    public static CruceDTO desde(ResultadoActualizado e) {
        return new CruceDTO(e.eventoId(), e.atletaId(), e.inscripcionId(), e.nombreAtleta(), e.dorsal(),
                e.categoria(), e.puntoControl(), e.km(), e.esMeta(), e.tiempoNetoMs(),
                e.posicionGeneral(), e.posicionCategoria(), e.estado(), e.recibidoEn());
    }
}
