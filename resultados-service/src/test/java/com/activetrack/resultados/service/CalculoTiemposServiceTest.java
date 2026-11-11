package com.activetrack.resultados.service;

import com.activetrack.events.AtletaCruzoPuntoControl;
import com.activetrack.events.ResultadoActualizado;
import com.activetrack.resultados.messaging.ResultadoEventPublisher;
import com.activetrack.resultados.model.EstadoResultado;
import com.activetrack.resultados.model.Resultado;
import com.activetrack.resultados.repository.ConsultaRepository;
import com.activetrack.resultados.repository.ConsultaRepository.InscripcionInfo;
import com.activetrack.resultados.repository.ConsultaRepository.PuntoInfo;
import com.activetrack.resultados.repository.PenalizacionRepository;
import com.activetrack.resultados.repository.ResultadoRepository;
import com.activetrack.resultados.service.LeaderboardService.Posiciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalculoTiemposServiceTest {

    private static final Instant LARGADA = Instant.parse("2026-11-15T11:00:00Z");

    private ResultadoRepository resultados;
    private ConsultaRepository consultas;
    private LeaderboardService leaderboard;
    private ResultadoEventPublisher publisher;
    private CalculoTiemposService service;

    private final InscripcionInfo info =
            new InscripcionInfo(7L, 7L, "Jorge Vargas", 107, 3L, "Varones 40+", 1L, LARGADA);
    private final PuntoInfo meta = new PuntoInfo(4L, 1L, "Meta", 42.195, 3, true);

    @BeforeEach
    void setUp() {
        resultados = mock(ResultadoRepository.class);
        consultas = mock(ConsultaRepository.class);
        leaderboard = mock(LeaderboardService.class);
        publisher = mock(ResultadoEventPublisher.class);
        service = new CalculoTiemposService(resultados, mock(PenalizacionRepository.class), consultas,
                leaderboard, publisher);

        Map<Integer, String> ruta = new LinkedHashMap<>();
        ruta.put(0, "Salida");
        ruta.put(1, "Control 10K");
        ruta.put(2, "Control 21K");
        ruta.put(3, "Meta");
        when(consultas.inscripcion(7L)).thenReturn(info);
        when(consultas.punto(4L)).thenReturn(meta);
        when(consultas.puntosDeRuta(1L)).thenReturn(ruta);
        when(consultas.marcaSalida(7L)).thenReturn(Optional.of(LARGADA.plusSeconds(30)));
        when(resultados.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leaderboard.actualizar(any(), any(), any())).thenReturn(new Posiciones(1L, 1L));
    }

    private AtletaCruzoPuntoControl cruceEnMeta(Instant marca) {
        return new AtletaCruzoPuntoControl(50L, 1L, 7L, 4L, 3, true, marca, marca);
    }

    @Test
    void calculaTiemposYPublicaResultadoFinalizado() {
        when(resultados.findByInscripcionId(7L)).thenReturn(Optional.empty());
        when(consultas.ordenesRecorridos(7L)).thenReturn(List.of(0, 1, 2, 3));
        Instant marca = LARGADA.plusSeconds(3 * 3600);

        service.procesar(cruceEnMeta(marca));

        ArgumentCaptor<ResultadoActualizado> captor = ArgumentCaptor.forClass(ResultadoActualizado.class);
        verify(publisher).publicar(captor.capture());
        ResultadoActualizado evento = captor.getValue();
        assertThat(evento.estado()).isEqualTo(EstadoResultado.FINALIZADO.name());
        assertThat(evento.tiempoBrutoMs()).isEqualTo(3 * 3600 * 1000L);
        assertThat(evento.tiempoNetoMs()).isEqualTo((3 * 3600 - 30) * 1000L);
        assertThat(evento.posicionGeneral()).isEqualTo(1L);
    }

    @Test
    void marcaObservadoSiOmitioUnPuntoDeControl() {
        when(resultados.findByInscripcionId(7L)).thenReturn(Optional.empty());
        when(consultas.ordenesRecorridos(7L)).thenReturn(List.of(0, 2, 3)); // sin 10K

        service.procesar(cruceEnMeta(LARGADA.plusSeconds(3 * 3600)));

        ArgumentCaptor<ResultadoActualizado> captor = ArgumentCaptor.forClass(ResultadoActualizado.class);
        verify(publisher).publicar(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(EstadoResultado.OBSERVADO.name());
    }

    @Test
    void noPublicaSiElAtletaEstaDescalificado() {
        Resultado dsq = new Resultado(7L);
        dsq.descalificar();
        when(resultados.findByInscripcionId(7L)).thenReturn(Optional.of(dsq));

        service.procesar(cruceEnMeta(LARGADA.plusSeconds(3 * 3600)));

        verify(publisher, never()).publicar(any());
    }
}
