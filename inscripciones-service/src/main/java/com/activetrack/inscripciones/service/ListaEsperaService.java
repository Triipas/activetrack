package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.messaging.InscripcionEventPublisher;
import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.model.Inscripcion;
import com.activetrack.inscripciones.repository.InscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Libera los cupos no pagados a tiempo y se los asigna a la lista de espera. */
@Service
public class ListaEsperaService {

    private static final Logger log = LoggerFactory.getLogger(ListaEsperaService.class);

    private final InscripcionRepository inscripciones;
    private final CupoService cupos;
    private final FichaService fichas;
    private final InscripcionEventPublisher publisher;
    private final Clock clock;
    private final Duration plazoPago;

    public ListaEsperaService(InscripcionRepository inscripciones, CupoService cupos, FichaService fichas,
                              InscripcionEventPublisher publisher, Clock clock,
                              @Value("${inscripciones.minutos-reserva}") long minutosReserva) {
        this.inscripciones = inscripciones;
        this.cupos = cupos;
        this.fichas = fichas;
        this.publisher = publisher;
        this.clock = clock;
        this.plazoPago = Duration.ofMinutes(minutosReserva);
    }

    /** Asigna los cupos libres de la categoría a los primeros de la lista de espera. */
    @Transactional
    public int promover(Long categoriaId) {
        int promovidos = 0;
        while (cupos.reservar(categoriaId)) {
            Optional<Inscripcion> siguiente = inscripciones.primeraEnEspera(categoriaId);
            if (siguiente.isEmpty()) {
                cupos.liberar(categoriaId);
                break;
            }
            Instant ahora = clock.instant();
            Inscripcion inscripcion = siguiente.get();
            inscripcion.promoverDesdeEspera(ahora.plus(plazoPago));
            publisher.reservada(fichas.de(inscripcion).eventoReservada());
            promovidos++;
        }
        return promovidos;
    }

    /** Marca como expiradas las reservas no pagadas y devuelve sus cupos. Retorna las categorías afectadas. */
    @Transactional
    public Set<Long> expirarVencidas() {
        Set<Long> categorias = new TreeSet<>();
        for (Inscripcion vencida : inscripciones.findByEstadoAndExpiraEnBefore(EstadoInscripcion.PENDIENTE_PAGO, clock.instant())) {
            vencida.expirar();
            cupos.liberar(vencida.getCategoriaId());
            categorias.add(vencida.getCategoriaId());
            log.info("Reserva {} expirada: cupo liberado en la categoría {}", vencida.getId(), vencida.getCategoriaId());
        }
        return categorias;
    }
}
