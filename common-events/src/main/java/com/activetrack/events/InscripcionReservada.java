package com.activetrack.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento publicado por el Servicio de Inscripciones cuando un atleta obtiene un cupo
 * y tiene un plazo para pagar.
 */
public record InscripcionReservada(
        Long inscripcionId,
        Long eventoId,
        String evento,
        String categoria,
        String atleta,
        String email,
        BigDecimal monto,
        Instant expiraEn) {
}
