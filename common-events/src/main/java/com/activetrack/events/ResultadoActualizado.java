package com.activetrack.events;

import java.time.Instant;

/**
 * Evento publicado por el Servicio de Resultados cada vez que cambia el resultado
 * de un atleta (nuevo cruce o penalización).
 */
public record ResultadoActualizado(
        Long eventoId,
        Long inscripcionId,
        Long atletaId,
        String nombreAtleta,
        int dorsal,
        String categoria,
        String puntoControl,
        double km,
        int ordenPunto,
        boolean esMeta,
        long tiempoNetoMs,
        long tiempoBrutoMs,
        Long posicionGeneral,
        Long posicionCategoria,
        String estado,
        Instant recibidoEn) {
}
