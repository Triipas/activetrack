package com.activetrack.inscripciones.service;

/** Recurso inexistente (responde 404). */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
