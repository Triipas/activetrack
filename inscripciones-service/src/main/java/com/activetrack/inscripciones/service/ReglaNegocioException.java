package com.activetrack.inscripciones.service;

/** La solicitud es válida pero incumple una regla del negocio (responde 422). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
