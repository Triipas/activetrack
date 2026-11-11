package com.activetrack.resultados.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadoTest {

    private final Instant marca = Instant.parse("2026-11-15T12:00:00Z");

    @Test
    void alCruzarLaMetaSinFaltantesQuedaFinalizado() {
        Resultado r = new Resultado(1L);
        r.registrarCruce(4L, 3, marca, true, false);

        assertThat(r.getEstado()).isEqualTo(EstadoResultado.FINALIZADO);
    }

    @Test
    void conPuntoFaltanteQuedaObservadoHastaQueElJuezDecida() {
        Resultado r = new Resultado(1L);
        r.registrarCruce(3L, 2, marca, false, true);
        r.registrarCruce(4L, 3, marca.plusSeconds(60), true, false);

        assertThat(r.getEstado()).isEqualTo(EstadoResultado.OBSERVADO);

        r.cerrarObservacion(true);
        assertThat(r.getEstado()).isEqualTo(EstadoResultado.FINALIZADO);
    }

    @Test
    void ignoraCrucesAntiguosYLosDeUnDescalificado() {
        Resultado r = new Resultado(1L);
        r.registrarCruce(3L, 2, marca, false, false);

        assertThat(r.esCruceNuevo(1)).isFalse();
        assertThat(r.esCruceNuevo(3)).isTrue();

        r.descalificar();
        assertThat(r.esCruceNuevo(3)).isFalse();
    }
}
