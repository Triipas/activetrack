package com.activetrack.resultados.service;

import com.activetrack.events.AtletaCruzoPuntoControl;
import com.activetrack.events.ResultadoActualizado;
import com.activetrack.resultados.messaging.ResultadoEventPublisher;
import com.activetrack.resultados.model.Resultado;
import com.activetrack.resultados.model.TipoPenalizacion;
import com.activetrack.resultados.repository.ConsultaRepository;
import com.activetrack.resultados.repository.ConsultaRepository.InscripcionInfo;
import com.activetrack.resultados.repository.ConsultaRepository.PuntoInfo;
import com.activetrack.resultados.repository.PenalizacionRepository;
import com.activetrack.resultados.repository.ResultadoRepository;
import com.activetrack.resultados.service.LeaderboardService.Posiciones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Procesa cada cruce: valida la secuencia de puntos, calcula tiempo bruto y neto,
 * actualiza el leaderboard y publica ResultadoActualizado.
 */
@Service
public class CalculoTiemposService {

    private static final Logger log = LoggerFactory.getLogger(CalculoTiemposService.class);

    private final ResultadoRepository resultados;
    private final PenalizacionRepository penalizaciones;
    private final ConsultaRepository consultas;
    private final LeaderboardService leaderboard;
    private final ResultadoEventPublisher publisher;

    public CalculoTiemposService(ResultadoRepository resultados, PenalizacionRepository penalizaciones,
                                 ConsultaRepository consultas, LeaderboardService leaderboard,
                                 ResultadoEventPublisher publisher) {
        this.resultados = resultados;
        this.penalizaciones = penalizaciones;
        this.consultas = consultas;
        this.leaderboard = leaderboard;
        this.publisher = publisher;
    }

    @Transactional
    public void procesar(AtletaCruzoPuntoControl cruce) {
        InscripcionInfo info = consultas.inscripcion(cruce.inscripcionId());
        PuntoInfo punto = consultas.punto(cruce.puntoControlId());
        Resultado resultado = resultados.findByInscripcionId(cruce.inscripcionId())
                .orElseGet(() -> new Resultado(cruce.inscripcionId()));

        if (!resultado.esCruceNuevo(punto.orden())) {
            log.info("Cruce ignorado (antiguo o atleta descalificado): inscripcion={} punto={}",
                    cruce.inscripcionId(), punto.nombre());
            return;
        }

        List<Integer> faltantes = ValidadorSecuencia.puntosFaltantes(
                consultas.puntosDeRuta(punto.rutaId()).keySet(),
                consultas.ordenesRecorridos(cruce.inscripcionId()),
                punto.orden());
        if (!faltantes.isEmpty()) {
            log.warn("Anomalía: dorsal {} cruzó {} sin pasar por los puntos {}", info.dorsal(), punto.nombre(), faltantes);
        }

        resultado.registrarCruce(punto.id(), punto.orden(), cruce.marcaTiempo(), punto.esMeta(), !faltantes.isEmpty());
        recalcularYPublicar(resultado, info, punto, cruce.recibidoEn());
    }

    /** Recalcula tiempos (incluye penalidades), actualiza el leaderboard y publica el cambio. */
    @Transactional
    public ResultadoActualizado recalcularYPublicar(Resultado resultado, InscripcionInfo info, PuntoInfo punto,
                                                    Instant recibidoEn) {
        long penalizacionSeg = resultado.getId() == null ? 0
                : penalizaciones.sumarSegundos(resultado.getId(), TipoPenalizacion.TIEMPO);
        Instant salidaChip = consultas.marcaSalida(info.inscripcionId()).orElse(null);
        resultado.actualizarTiempos(CalculadoraTiempos.calcular(
                info.horaSalidaOficial(), salidaChip, resultado.getUltimaMarca(), penalizacionSeg));

        Resultado guardado = resultados.save(resultado);
        Posiciones posiciones = leaderboard.actualizar(info, guardado, punto);
        guardado.actualizarPosiciones(posiciones.general(), posiciones.categoria());

        ResultadoActualizado evento = new ResultadoActualizado(
                info.eventoId(), info.inscripcionId(), info.atletaId(), info.nombre(), info.dorsal(),
                info.categoria(), punto.nombre(), punto.km(), punto.orden(), punto.esMeta(),
                guardado.getTiempoNetoMs(), guardado.getTiempoBrutoMs(),
                posiciones.general(), posiciones.categoria(), guardado.getEstado().name(), recibidoEn);
        publisher.publicar(evento);
        return evento;
    }
}
