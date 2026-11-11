package com.activetrack.cronometraje.service;

/** Respuesta a la antena: si la lectura se publicó o se descartó por duplicada. */
public record ResultadoLectura(Long lecturaId, String estado) {

    public static final String PUBLICADA = "PUBLICADA";
    public static final String DUPLICADA = "DUPLICADA";

    public static ResultadoLectura publicada(Long lecturaId) {
        return new ResultadoLectura(lecturaId, PUBLICADA);
    }

    public static ResultadoLectura duplicada() {
        return new ResultadoLectura(null, DUPLICADA);
    }
}
