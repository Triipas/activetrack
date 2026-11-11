package com.activetrack.resultados.service;

import com.activetrack.resultados.model.EstadoResultado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderboardPuntajeTest {

    @Test
    void quienLlegoMasLejosVaPrimeroAunqueTengaMasTiempo() {
        double enMeta = LeaderboardService.puntaje(3, 9_000_000, EstadoResultado.FINALIZADO);
        double en21k = LeaderboardService.puntaje(2, 4_000_000, EstadoResultado.EN_CARRERA);

        assertThat(enMeta).isLessThan(en21k);
    }

    @Test
    void aIgualPuntoGanaElMenorTiempoNeto() {
        double rapido = LeaderboardService.puntaje(3, 8_000_000, EstadoResultado.FINALIZADO);
        double lento = LeaderboardService.puntaje(3, 8_000_001, EstadoResultado.FINALIZADO);

        assertThat(rapido).isLessThan(lento);
    }

    @Test
    void elDescalificadoVaAlFinal() {
        double dsq = LeaderboardService.puntaje(3, 1_000, EstadoResultado.DSQ);
        double recienSalio = LeaderboardService.puntaje(0, 0, EstadoResultado.EN_CARRERA);

        assertThat(dsq).isGreaterThan(recienSalio);
    }
}
