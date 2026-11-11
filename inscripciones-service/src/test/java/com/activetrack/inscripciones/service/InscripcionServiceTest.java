package com.activetrack.inscripciones.service;

import com.activetrack.events.InscripcionReservada;
import com.activetrack.inscripciones.messaging.InscripcionEventPublisher;
import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.model.Evento;
import com.activetrack.inscripciones.model.Tarifa;
import com.activetrack.inscripciones.repository.AtletaRepository;
import com.activetrack.inscripciones.repository.CategoriaRepository;
import com.activetrack.inscripciones.repository.EventoRepository;
import com.activetrack.inscripciones.repository.InscripcionRepository;
import com.activetrack.inscripciones.repository.TarifaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InscripcionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T15:00:00Z");

    private CategoriaRepository categorias;
    private InscripcionRepository inscripciones;
    private AtletaRepository atletas;
    private CupoService cupos;
    private FichaService fichas;
    private InscripcionEventPublisher publisher;
    private InscripcionService service;

    private final Categoria damas40 = conId(new Categoria(2L, "Damas 40+", 40, 99, "F", 3), 8L);
    private final Evento evento = conId(new Evento("Carrera 10K", "CARRERA", Instant.parse("2027-03-21T12:00:00Z")), 2L);
    private final Tarifa earlyBird = conId(new Tarifa(2L, "Early Bird", new BigDecimal("80"),
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)), 3L);

    private final InscripcionService.Solicitud solicitud = new InscripcionService.Solicitud(
            8L, "Rosa Luna", "40000001", LocalDate.of(1980, 5, 5), "F", "rosa@example.com");

    @BeforeEach
    void setUp() {
        categorias = mock(CategoriaRepository.class);
        EventoRepository eventos = mock(EventoRepository.class);
        TarifaRepository tarifas = mock(TarifaRepository.class);
        atletas = mock(AtletaRepository.class);
        inscripciones = mock(InscripcionRepository.class);
        cupos = mock(CupoService.class);
        fichas = mock(FichaService.class);
        publisher = mock(InscripcionEventPublisher.class);
        service = new InscripcionService(categorias, eventos, tarifas, atletas, inscripciones, cupos, fichas,
                publisher, Clock.fixed(AHORA, ZoneOffset.UTC), 10);

        when(categorias.findById(8L)).thenReturn(Optional.of(damas40));
        when(eventos.findById(2L)).thenReturn(Optional.of(evento));
        when(tarifas.findByEventoIdOrderByVigenteDesde(2L)).thenReturn(List.of(earlyBird));
        when(atletas.findByDocumento("40000001")).thenReturn(Optional.empty());
        when(atletas.save(any())).thenAnswer(inv -> conId(inv.getArgument(0), 50L));
        when(inscripciones.save(any())).thenAnswer(inv -> conId(inv.getArgument(0), 900L));
    }

    @Test
    void conCupoReservaYPublicaInscripcionReservada() {
        when(cupos.reservar(8L)).thenReturn(true);

        InscripcionService.Resultado r = service.inscribir(solicitud);

        assertThat(r.estado()).isEqualTo(EstadoInscripcion.PENDIENTE_PAGO.name());
        assertThat(r.monto()).isEqualByComparingTo("80");
        assertThat(r.expiraEn()).isEqualTo(AHORA.plusSeconds(600));
        ArgumentCaptor<InscripcionReservada> evento = ArgumentCaptor.forClass(InscripcionReservada.class);
        verify(publisher).reservada(evento.capture());
        assertThat(evento.getValue().inscripcionId()).isEqualTo(900L);
    }

    @Test
    void sinCupoQuedaEnListaDeEsperaSinPublicar() {
        when(cupos.reservar(8L)).thenReturn(false);
        when(fichas.posicionEspera(any())).thenReturn(4L);

        InscripcionService.Resultado r = service.inscribir(solicitud);

        assertThat(r.estado()).isEqualTo(EstadoInscripcion.EN_ESPERA.name());
        assertThat(r.posicionEspera()).isEqualTo(4L);
        verify(publisher, never()).reservada(any());
    }

    @Test
    void noPermiteDosInscripcionesDelMismoAtletaEnElEvento() {
        when(inscripciones.tieneInscripcion(anyLong(), eq(2L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.inscribir(solicitud)).isInstanceOf(ReglaNegocioException.class);
        verify(cupos, never()).reservar(any());
    }

    @Test
    void rechazaSiLasInscripcionesEstanCerradas() {
        evento.cambiarEstado("EN_CURSO");

        assertThatThrownBy(() -> service.inscribir(solicitud))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cerradas");
    }

    @Test
    void rechazaAtletaQueNoCorrespondeALaCategoria() {
        InscripcionService.Solicitud joven = new InscripcionService.Solicitud(
                8L, "Lucía", "40000002", LocalDate.of(2000, 1, 1), "F", "lu@example.com");

        assertThatThrownBy(() -> service.inscribir(joven)).isInstanceOf(ReglaNegocioException.class);
        verify(cupos, never()).reservar(any());
    }

    static <T> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }
}
