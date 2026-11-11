package com.activetrack.resultados.service;

/**
 * @param brutoMs tiempo desde la largada oficial (pistola)
 * @param netoMs  tiempo desde que el chip pisó la alfombra de salida
 */
public record Tiempos(long brutoMs, long netoMs) {
}
