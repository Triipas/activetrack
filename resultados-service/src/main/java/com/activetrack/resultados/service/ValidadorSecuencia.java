package com.activetrack.resultados.service;

import java.util.Collection;
import java.util.List;

/**
 * Detecta puntos de control omitidos (ej. el atleta cruzó la meta pero no el control de 10 km).
 * La salida (orden 0) no se exige: si no se leyó, se usa la largada oficial.
 */
public final class ValidadorSecuencia {

    private ValidadorSecuencia() {
    }

    public static List<Integer> puntosFaltantes(Collection<Integer> ordenesRuta,
                                                Collection<Integer> ordenesRecorridos,
                                                int ordenActual) {
        return ordenesRuta.stream()
                .filter(orden -> orden > 0 && orden < ordenActual)
                .filter(orden -> !ordenesRecorridos.contains(orden))
                .sorted()
                .toList();
    }
}
