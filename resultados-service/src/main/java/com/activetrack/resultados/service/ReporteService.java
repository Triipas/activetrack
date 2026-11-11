package com.activetrack.resultados.service;

import com.activetrack.resultados.repository.ConsultaRepository;
import com.activetrack.resultados.repository.ConsultaRepository.FilaResultado;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

/** Historial de resultados del atleta y exportación de resultados del evento. */
@Service
public class ReporteService {

    public record ResultadoHistorico(Long eventoId, String evento, Instant fecha, String categoria, Integer dorsal,
                                     String estado, String ultimoPunto, Double km, Long tiempoNetoMs,
                                     Long tiempoBrutoMs, Long posicionGeneral, Long posicionCategoria,
                                     Long ritmoSegundosPorKm) {
    }

    public record Historial(String documento, String atleta, int eventos, int finalizados,
                            Long mejorRitmoSegundosPorKm, List<ResultadoHistorico> resultados) {
    }

    private final ConsultaRepository consultas;

    public ReporteService(ConsultaRepository consultas) {
        this.consultas = consultas;
    }

    public Historial historial(String documento) {
        String atleta = consultas.nombreAtleta(documento)
                .orElseThrow(() -> new NoSuchElementException("No hay un atleta con ese documento"));
        List<ResultadoHistorico> resultados = consultas.resultadosDeAtleta(documento).stream()
                .map(f -> new ResultadoHistorico(f.eventoId(), f.evento(), f.fecha(), f.categoria(), f.dorsal(),
                        f.estado(), f.ultimoPunto(), f.km(), f.tiempoNetoMs(), f.tiempoBrutoMs(),
                        f.posicionGeneral(), f.posicionCategoria(), ritmo(f.tiempoNetoMs(), f.km())))
                .toList();
        Long mejorRitmo = resultados.stream()
                .filter(r -> "FINALIZADO".equals(r.estado()) && r.ritmoSegundosPorKm() != null)
                .map(ResultadoHistorico::ritmoSegundosPorKm).min(Long::compare).orElse(null);
        int finalizados = (int) resultados.stream().filter(r -> "FINALIZADO".equals(r.estado())).count();
        return new Historial(documento, atleta, resultados.size(), finalizados, mejorRitmo, resultados);
    }

    /** Resultados del evento en CSV (separador ';' para Excel en español). */
    public String resultadosCsv(Long eventoId) {
        StringBuilder csv = new StringBuilder(
                "posicion;dorsal;atleta;documento;categoria;posicion_categoria;ultimo_punto;tiempo_neto;tiempo_bruto;estado\n");
        for (FilaResultado f : consultas.resultadosDeEvento(eventoId)) {
            csv.append(String.join(";", texto(f.posicionGeneral()), texto(f.dorsal()), limpiar(f.atleta()),
                    f.documento(), limpiar(f.categoria()), texto(f.posicionCategoria()), limpiar(f.ultimoPunto()),
                    formato(f.tiempoNetoMs()), formato(f.tiempoBrutoMs()), f.estado())).append('\n');
        }
        return csv.toString();
    }

    /** Segundos por kilómetro (ej. 300 = 5:00 min/km). */
    static Long ritmo(Long netoMs, Double km) {
        if (netoMs == null || km == null || km <= 0) {
            return null;
        }
        return Math.round(netoMs / 1000.0 / km);
    }

    static String formato(Long ms) {
        if (ms == null) {
            return "";
        }
        Duration d = Duration.ofMillis(ms);
        return String.format("%d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }

    private static String texto(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.replace(';', ',');
    }
}
