package com.activetrack.resultados.service;

import com.activetrack.resultados.repository.ConsultaRepository;
import com.activetrack.resultados.repository.ConsultaRepository.FilaResultado;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReporteServiceTest {

    @Test
    void calculaElRitmoEnSegundosPorKilometro() {
        // 42,195 km en 3h 30m = 12 600 s → 299 s/km (4:59 min/km)
        assertThat(ReporteService.ritmo(12_600_000L, 42.195)).isEqualTo(299L);
        assertThat(ReporteService.ritmo(1_000L, 0.0)).isNull();
    }

    @Test
    void elHistorialResumeEventosFinalizadosYMejorRitmo() {
        ConsultaRepository consultas = mock(ConsultaRepository.class);
        when(consultas.nombreAtleta("70000001")).thenReturn(Optional.of("Carlos Mamani"));
        when(consultas.resultadosDeAtleta("70000001")).thenReturn(List.of(
                fila("Maratón", "FINALIZADO", 42.195, 12_600_000L),
                fila("10K", "FINALIZADO", 10.0, 2_700_000L),
                fila("21K", "DSQ", 21.097, 5_000_000L)));

        ReporteService.Historial h = new ReporteService(consultas).historial("70000001");

        assertThat(h.eventos()).isEqualTo(3);
        assertThat(h.finalizados()).isEqualTo(2);
        assertThat(h.mejorRitmoSegundosPorKm()).isEqualTo(270L); // 10K en 45 min
    }

    @Test
    void elCsvIncluyeTiemposFormateados() {
        ConsultaRepository consultas = mock(ConsultaRepository.class);
        when(consultas.resultadosDeEvento(1L)).thenReturn(List.of(fila("Maratón", "FINALIZADO", 42.195, 8_130_000L)));

        String csv = new ReporteService(consultas).resultadosCsv(1L);

        assertThat(csv.lines()).hasSize(2);
        assertThat(csv).contains("2:15:30");
    }

    private static FilaResultado fila(String evento, String estado, double km, long netoMs) {
        return new FilaResultado(1L, evento, Instant.parse("2026-11-15T11:00:00Z"), "Carlos Mamani", "70000001",
                "Varones 18-39", 101, estado, "Meta", km, netoMs, netoMs + 20_000, 1L, 1L);
    }
}
