package com.activetrack.cronometraje.service;

import com.activetrack.cronometraje.messaging.CruceEventPublisher;
import com.activetrack.cronometraje.model.Inscripcion;
import com.activetrack.cronometraje.model.LecturaRfid;
import com.activetrack.cronometraje.model.PuntoControl;
import com.activetrack.cronometraje.model.Ruta;
import com.activetrack.cronometraje.repository.InscripcionRepository;
import com.activetrack.cronometraje.repository.LecturaRfidRepository;
import com.activetrack.cronometraje.repository.PuntoControlRepository;
import com.activetrack.events.AtletaCruzoPuntoControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LecturaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T11:45:00Z");

    private PuntoControlRepository puntoRepo;
    private InscripcionRepository inscripcionRepo;
    private LecturaRfidRepository lecturaRepo;
    private CruceEventPublisher publisher;
    private LecturaService service;

    private final PuntoControl control10k =
            new PuntoControl(2L, new Ruta(1L, 1L, "42K"), "Control 10K", new BigDecimal("10"), 1, false, "ANT-10K");
    private final Inscripcion inscripcion = new Inscripcion(7L, "CHIP-107", 107, Inscripcion.CONFIRMADA);

    @BeforeEach
    void setUp() {
        puntoRepo = mock(PuntoControlRepository.class);
        inscripcionRepo = mock(InscripcionRepository.class);
        lecturaRepo = mock(LecturaRfidRepository.class);
        publisher = mock(CruceEventPublisher.class);
        service = new LecturaService(puntoRepo, inscripcionRepo, lecturaRepo, publisher,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        when(puntoRepo.findByAntenaId("ANT-10K")).thenReturn(Optional.of(control10k));
        when(inscripcionRepo.findByChipId("CHIP-107")).thenReturn(Optional.of(inscripcion));
        when(lecturaRepo.save(any())).thenAnswer(inv -> {
            LecturaRfid l = inv.getArgument(0);
            ReflectionTestUtils.setField(l, "id", 99L);
            return l;
        });
    }

    @Test
    void publicaElEventoDeCruceConLosDatosDelPunto() {
        Instant marca = Instant.parse("2026-11-15T11:44:59Z");

        ResultadoLectura resultado = service.registrar("CHIP-107", "ANT-10K", marca);

        assertThat(resultado.estado()).isEqualTo(ResultadoLectura.PUBLICADA);
        ArgumentCaptor<AtletaCruzoPuntoControl> captor = ArgumentCaptor.forClass(AtletaCruzoPuntoControl.class);
        verify(publisher).publicar(captor.capture());
        AtletaCruzoPuntoControl evento = captor.getValue();
        assertThat(evento.lecturaId()).isEqualTo(99L);
        assertThat(evento.eventoId()).isEqualTo(1L);
        assertThat(evento.inscripcionId()).isEqualTo(7L);
        assertThat(evento.ordenPunto()).isEqualTo(1);
        assertThat(evento.marcaTiempo()).isEqualTo(marca);
        assertThat(evento.recibidoEn()).isEqualTo(AHORA);
    }

    @Test
    void sinMarcaDeTiempoUsaLaHoraDeRecepcion() {
        service.registrar("CHIP-107", "ANT-10K", null);

        ArgumentCaptor<AtletaCruzoPuntoControl> captor = ArgumentCaptor.forClass(AtletaCruzoPuntoControl.class);
        verify(publisher).publicar(captor.capture());
        assertThat(captor.getValue().marcaTiempo()).isEqualTo(AHORA);
    }

    @Test
    void descartaLecturaDuplicadaSinPublicar() {
        when(lecturaRepo.existsByInscripcionIdAndPuntoControlId(7L, 2L)).thenReturn(true);

        ResultadoLectura resultado = service.registrar("CHIP-107", "ANT-10K", null);

        assertThat(resultado.estado()).isEqualTo(ResultadoLectura.DUPLICADA);
        verify(publisher, never()).publicar(any());
    }

    @Test
    void rechazaAntenaDesconocida() {
        assertThatThrownBy(() -> service.registrar("CHIP-107", "ANT-X", null))
                .isInstanceOf(LecturaInvalidaException.class)
                .hasMessageContaining("ANT-X");
    }

    @Test
    void rechazaInscripcionNoConfirmada() {
        when(inscripcionRepo.findByChipId("CHIP-200"))
                .thenReturn(Optional.of(new Inscripcion(20L, "CHIP-200", 200, "PENDIENTE_PAGO")));

        assertThatThrownBy(() -> service.registrar("CHIP-200", "ANT-10K", null))
                .isInstanceOf(LecturaInvalidaException.class);
        verify(publisher, never()).publicar(any());
    }
}
