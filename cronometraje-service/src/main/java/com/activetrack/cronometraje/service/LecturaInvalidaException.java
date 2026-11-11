package com.activetrack.cronometraje.service;

public class LecturaInvalidaException extends RuntimeException {

    public LecturaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
