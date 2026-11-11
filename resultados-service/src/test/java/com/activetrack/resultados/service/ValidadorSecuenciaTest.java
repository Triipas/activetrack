package com.activetrack.resultados.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorSecuenciaTest {

    // Salida(0), 10K(1), 21K(2), Meta(3)
    private static final List<Integer> RUTA = List.of(0, 1, 2, 3);

    @Test
    void secuenciaCompletaNoTieneFaltantes() {
        assertThat(ValidadorSecuencia.puntosFaltantes(RUTA, List.of(0, 1, 2, 3), 3)).isEmpty();
    }

    @Test
    void detectaElControlOmitido() {
        // Cruzó la meta sin pasar por el control de 10K
        assertThat(ValidadorSecuencia.puntosFaltantes(RUTA, List.of(0, 2, 3), 3)).containsExactly(1);
    }

    @Test
    void laSalidaNoEsObligatoria() {
        assertThat(ValidadorSecuencia.puntosFaltantes(RUTA, List.of(1), 1)).isEmpty();
    }

    @Test
    void soloRevisaPuntosAnterioresAlActual() {
        assertThat(ValidadorSecuencia.puntosFaltantes(RUTA, List.of(0, 1), 1)).isEmpty();
    }
}
