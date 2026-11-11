package com.activetrack.inscripciones.service;

import com.activetrack.events.InscripcionConfirmada;
import com.activetrack.inscripciones.messaging.InscripcionEventPublisher;
import com.activetrack.inscripciones.model.Atleta;
import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.model.Evento;
import com.activetrack.inscripciones.model.Inscripcion;
import com.activetrack.inscripciones.model.Pago;
import com.activetrack.inscripciones.model.Tarifa;
import com.activetrack.inscripciones.repository.InscripcionRepository;
import com.activetrack.inscripciones.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.activetrack.inscripciones.service.InscripcionServiceTest.conId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagoServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T15:00:00Z");

    private PagoRepository pagos;
    private InscripcionRepository inscripciones;
    private PasarelaPagos pasarela;
    private CupoService cupos;
    private ListaEsperaService listaEspera;
    private InscripcionEventPublisher publisher;
    private PagoService service;

    private Inscripcion inscripcion;
    private Pago pago;

    @BeforeEach
    void setUp() {
        pagos = mock(PagoRepository.class);
        inscripciones = mock(InscripcionRepository.class);
        FichaService fichas = mock(FichaService.class);
        pasarela = mock(PasarelaPagos.class);
        cupos = mock(CupoService.class);
        listaEspera = mock(ListaEsperaService.class);
        publisher = mock(InscripcionEventPublisher.class);
        service = new PagoService(pagos, inscripciones, fichas, pasarela, cupos, listaEspera, publisher,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        inscripcion = conId(Inscripcion.reservada(50L, 8L, 3L, AHORA, AHORA.plusSeconds(600)), 900L);
        pago = new Pago(900L, new BigDecimal("80"), "AT-REF-1", AHORA);
        when(pagos.findByReferenciaPasarela("AT-REF-1")).thenReturn(Optional.of(pago));
        when(inscripciones.findById(900L)).thenReturn(Optional.of(inscripcion));
        when(inscripciones.siguienteDorsal()).thenReturn(205);
        when(fichas.de(any())).thenAnswer(inv -> new FichaInscripcion(inv.getArgument(0),
                new Atleta("Rosa Luna", "40000001", LocalDate.of(1980, 5, 5), "F", "rosa@example.com"),
                new Categoria(2L, "Damas 40+", 40, 99, "F", 3),
                conId(new Evento("Carrera 10K", "CARRERA", Instant.parse("2027-03-21T12:00:00Z")), 2L),
                new Tarifa(2L, "Early Bird", new BigDecimal("80"), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31))));
    }

    @Test
    void pagoAprobadoConfirmaAsignaDorsalYPublica() {
        service.procesarNotificacion("AT-REF-1", true);

        assertThat(pago.getEstado()).isEqualTo(Pago.Estado.APROBADO);
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CONFIRMADA);
        assertThat(inscripcion.getDorsal()).isEqualTo(205);
        assertThat(inscripcion.getChipId()).isEqualTo("CHIP-205");
        ArgumentCaptor<InscripcionConfirmada> evento = ArgumentCaptor.forClass(InscripcionConfirmada.class);
        verify(publisher).confirmada(evento.capture());
        assertThat(evento.getValue().codigoQr()).startsWith("AT-900-");
    }

    @Test
    void laNotificacionRepetidaNoCambiaNada() {
        service.procesarNotificacion("AT-REF-1", true);
        service.procesarNotificacion("AT-REF-1", true);

        verify(publisher).confirmada(any());
    }

    @Test
    void pagoQueLlegaTrasExpirarSeReembolsa() {
        inscripcion.expirar();

        service.procesarNotificacion("AT-REF-1", true);

        assertThat(pago.getEstado()).isEqualTo(Pago.Estado.REEMBOLSADO);
        verify(pasarela).reembolsar("AT-REF-1", new BigDecimal("80"));
        verify(publisher, never()).confirmada(any());
    }

    @Test
    void reembolsoAnulaLiberaCupoYPromueveListaDeEspera() {
        service.procesarNotificacion("AT-REF-1", true);
        when(pagos.findFirstByInscripcionIdAndEstado(900L, Pago.Estado.APROBADO)).thenReturn(Optional.of(pago));

        service.reembolsar(900L);

        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.ANULADA);
        assertThat(pago.getEstado()).isEqualTo(Pago.Estado.REEMBOLSADO);
        verify(cupos).liberar(8L);
        verify(listaEspera).promover(8L);
    }

    @Test
    void soloSeReembolsanInscripcionesConfirmadas() {
        assertThatThrownBy(() -> service.reembolsar(900L)).isInstanceOf(ReglaNegocioException.class);
    }
}
