package com.activetrack.inscripciones.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InscripcionTest {

    private final Instant ahora = Instant.parse("2026-11-10T15:00:00Z");

    @Test
    void cicloCompletoReservaPagoYReembolso() {
        Inscripcion i = Inscripcion.reservada(1L, 5L, 3L, ahora, ahora.plusSeconds(600));
        assertThat(i.vencida(ahora.plusSeconds(599))).isFalse();
        assertThat(i.vencida(ahora.plusSeconds(600))).isTrue();

        i.confirmar(200, "CHIP-200", "AT-1-ABCD");
        assertThat(i.getEstado()).isEqualTo(EstadoInscripcion.CONFIRMADA);
        assertThat(i.getDorsal()).isEqualTo(200);
        assertThat(i.getExpiraEn()).isNull();

        i.anular();
        assertThat(i.getEstado()).isEqualTo(EstadoInscripcion.ANULADA);
    }

    @Test
    void desdeLaListaDeEsperaRecibePlazoParaPagar() {
        Inscripcion i = Inscripcion.enEspera(1L, 5L, 3L, ahora);
        i.promoverDesdeEspera(ahora.plusSeconds(600));

        assertThat(i.getEstado()).isEqualTo(EstadoInscripcion.PENDIENTE_PAGO);
        assertThat(i.getExpiraEn()).isEqualTo(ahora.plusSeconds(600));
    }

    @Test
    void noPermiteTransicionesInvalidas() {
        Inscripcion espera = Inscripcion.enEspera(1L, 5L, 3L, ahora);
        assertThatThrownBy(() -> espera.confirmar(201, "CHIP-201", "QR")).isInstanceOf(IllegalStateException.class);

        Inscripcion reservada = Inscripcion.reservada(1L, 5L, 3L, ahora, ahora.plusSeconds(600));
        reservada.expirar();
        assertThatThrownBy(() -> reservada.confirmar(202, "CHIP-202", "QR")).isInstanceOf(IllegalStateException.class);
    }
}
