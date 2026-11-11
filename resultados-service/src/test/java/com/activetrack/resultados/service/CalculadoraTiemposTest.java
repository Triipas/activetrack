package com.activetrack.resultados.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraTiemposTest {

    private static final Instant LARGADA = Instant.parse("2026-11-15T11:00:00Z");

    @Test
    void netoSeMideDesdeLaSalidaDelChipYBrutoDesdeLaLargada() {
        Instant salidaChip = LARGADA.plusSeconds(45);           // tardó 45 s en llegar a la alfombra
        Instant meta = LARGADA.plusSeconds(2 * 3600 + 10 * 60); // 2h10m después de la pistola

        Tiempos t = CalculadoraTiempos.calcular(LARGADA, salidaChip, meta, 0);

        assertThat(t.brutoMs()).isEqualTo((2 * 3600 + 10 * 60) * 1000L);
        assertThat(t.netoMs()).isEqualTo((2 * 3600 + 10 * 60 - 45) * 1000L);
    }

    @Test
    void sinLecturaDeSalidaElNetoEsIgualAlBruto() {
        Tiempos t = CalculadoraTiempos.calcular(LARGADA, null, LARGADA.plusSeconds(600), 0);

        assertThat(t.netoMs()).isEqualTo(t.brutoMs()).isEqualTo(600_000L);
    }

    @Test
    void laPenalidadSeSumaAAmbosTiempos() {
        Tiempos t = CalculadoraTiempos.calcular(LARGADA, LARGADA.plusSeconds(10), LARGADA.plusSeconds(1000), 30);

        assertThat(t.brutoMs()).isEqualTo(1_030_000L);
        assertThat(t.netoMs()).isEqualTo(1_020_000L);
    }

    @Test
    void rechazaCruceAnteriorALaLargada() {
        assertThatThrownBy(() -> CalculadoraTiempos.calcular(LARGADA, null, LARGADA.minusSeconds(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
