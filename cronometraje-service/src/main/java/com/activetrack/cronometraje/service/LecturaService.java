package com.activetrack.cronometraje.service;

import com.activetrack.cronometraje.messaging.CruceEventPublisher;
import com.activetrack.cronometraje.model.Inscripcion;
import com.activetrack.cronometraje.model.LecturaRfid;
import com.activetrack.cronometraje.model.PuntoControl;
import com.activetrack.cronometraje.repository.InscripcionRepository;
import com.activetrack.cronometraje.repository.LecturaRfidRepository;
import com.activetrack.cronometraje.repository.PuntoControlRepository;
import com.activetrack.events.AtletaCruzoPuntoControl;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Registra una lectura RFID y publica el evento de cruce.
 * No calcula tiempos: eso lo hace el Servicio de Resultados al consumir el evento,
 * así la antena recibe su confirmación en milisegundos.
 */
@Service
public class LecturaService {

    private final PuntoControlRepository puntoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final LecturaRfidRepository lecturaRepository;
    private final CruceEventPublisher publisher;
    private final Clock clock;

    public LecturaService(PuntoControlRepository puntoRepository,
                          InscripcionRepository inscripcionRepository,
                          LecturaRfidRepository lecturaRepository,
                          CruceEventPublisher publisher,
                          Clock clock) {
        this.puntoRepository = puntoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.lecturaRepository = lecturaRepository;
        this.publisher = publisher;
        this.clock = clock;
    }

    public ResultadoLectura registrar(String chipId, String antenaId, Instant marcaTiempo) {
        Instant recibidoEn = clock.instant();

        PuntoControl punto = puntoRepository.findByAntenaId(antenaId)
                .orElseThrow(() -> new LecturaInvalidaException("Antena no registrada: " + antenaId));
        Inscripcion inscripcion = inscripcionRepository.findByChipId(chipId)
                .orElseThrow(() -> new LecturaInvalidaException("Chip no registrado: " + chipId));
        if (!inscripcion.estaConfirmada()) {
            throw new LecturaInvalidaException("La inscripción del chip " + chipId + " no está confirmada");
        }

        // Una antena detecta el mismo chip varias veces al pasar: solo vale la primera lectura.
        if (lecturaRepository.existsByInscripcionIdAndPuntoControlId(inscripcion.getId(), punto.getId())) {
            return ResultadoLectura.duplicada();
        }

        Instant marca = marcaTiempo != null ? marcaTiempo : recibidoEn;
        LecturaRfid lectura;
        try {
            lectura = lecturaRepository.save(
                    new LecturaRfid(inscripcion.getId(), punto.getId(), antenaId, marca, recibidoEn));
        } catch (DataIntegrityViolationException e) {
            // Dos lecturas simultáneas del mismo chip: la restricción UNIQUE deja pasar solo una.
            return ResultadoLectura.duplicada();
        }

        publisher.publicar(new AtletaCruzoPuntoControl(
                lectura.getId(),
                punto.getRuta().getEventoId(),
                inscripcion.getId(),
                punto.getId(),
                punto.getOrden(),
                punto.isEsMeta(),
                marca,
                recibidoEn));

        return ResultadoLectura.publicada(lectura.getId());
    }
}
