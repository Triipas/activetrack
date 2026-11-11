package com.activetrack.resultados.service;

import com.activetrack.events.ResultadoActualizado;
import com.activetrack.resultados.model.EstadoResultado;
import com.activetrack.resultados.model.Penalizacion;
import com.activetrack.resultados.model.Resultado;
import com.activetrack.resultados.model.TipoPenalizacion;
import com.activetrack.resultados.repository.ConsultaRepository;
import com.activetrack.resultados.repository.ConsultaRepository.InscripcionInfo;
import com.activetrack.resultados.repository.ConsultaRepository.PuntoInfo;
import com.activetrack.resultados.repository.PenalizacionRepository;
import com.activetrack.resultados.repository.ResultadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Lista las anomalías detectadas y aplica las decisiones del juez (DSQ, penalidad o sin sanción). */
@Service
public class DisputaService {

    public record Anomalia(Long inscripcionId, int dorsal, String nombre, String categoria,
                           String ultimoPunto, List<String> puntosFaltantes, Long tiempoNetoMs) {
    }

    public record DecisionJuez(TipoPenalizacion tipo, int segundos, String motivo, String juez) {
    }

    private final ResultadoRepository resultados;
    private final PenalizacionRepository penalizaciones;
    private final ConsultaRepository consultas;
    private final CalculoTiemposService calculo;
    private final Clock clock;

    public DisputaService(ResultadoRepository resultados, PenalizacionRepository penalizaciones,
                          ConsultaRepository consultas, CalculoTiemposService calculo, Clock clock) {
        this.resultados = resultados;
        this.penalizaciones = penalizaciones;
        this.consultas = consultas;
        this.calculo = calculo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Anomalia> anomalias(Long eventoId) {
        List<Anomalia> lista = new ArrayList<>();
        for (Long inscripcionId : consultas.inscripcionesObservadas(eventoId)) {
            Resultado r = resultados.findByInscripcionId(inscripcionId).orElseThrow();
            InscripcionInfo info = consultas.inscripcion(inscripcionId);
            PuntoInfo ultimo = consultas.punto(r.getUltimoPuntoId());
            Map<Integer, String> puntos = consultas.puntosDeRuta(ultimo.rutaId());
            List<String> faltantes = ValidadorSecuencia.puntosFaltantes(
                            puntos.keySet(), consultas.ordenesRecorridos(inscripcionId), ultimo.orden())
                    .stream().map(puntos::get).toList();
            lista.add(new Anomalia(inscripcionId, info.dorsal(), info.nombre(), info.categoria(),
                    ultimo.nombre(), faltantes, r.getTiempoNetoMs()));
        }
        return lista;
    }

    @Transactional
    public ResultadoActualizado aplicarDecision(Long inscripcionId, DecisionJuez decision) {
        Resultado resultado = resultados.findByInscripcionId(inscripcionId)
                .orElseThrow(() -> new NoSuchElementException("El atleta aún no registra cruces"));
        if (resultado.getEstado() == EstadoResultado.DSQ) {
            throw new IllegalStateException("El atleta ya está descalificado");
        }
        if (decision.tipo() == TipoPenalizacion.TIEMPO && decision.segundos() <= 0) {
            throw new IllegalArgumentException("La penalidad de tiempo debe ser mayor a 0 segundos");
        }

        int segundos = decision.tipo() == TipoPenalizacion.TIEMPO ? decision.segundos() : 0;
        penalizaciones.save(new Penalizacion(resultado.getId(), decision.tipo(), segundos,
                decision.motivo(), decision.juez(), clock.instant()));

        PuntoInfo ultimo = consultas.punto(resultado.getUltimoPuntoId());
        if (decision.tipo() == TipoPenalizacion.DSQ) {
            resultado.descalificar();
        } else {
            resultado.cerrarObservacion(ultimo.esMeta());
        }
        return calculo.recalcularYPublicar(resultado, consultas.inscripcion(inscripcionId), ultimo, clock.instant());
    }
}
