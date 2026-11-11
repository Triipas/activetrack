package com.activetrack.events;

/**
 * Evento publicado por el Servicio de Inscripciones cuando se aprueba el pago.
 * Notificaciones lo usa para enviar el kit digital (código QR).
 */
public record InscripcionConfirmada(
        Long inscripcionId,
        Long eventoId,
        String evento,
        String categoria,
        String atleta,
        String email,
        int dorsal,
        String chipId,
        String codigoQr) {
}
