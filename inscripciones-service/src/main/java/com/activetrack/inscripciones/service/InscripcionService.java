package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.messaging.InscripcionEventPublisher;
import com.activetrack.inscripciones.model.Atleta;
import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.model.Evento;
import com.activetrack.inscripciones.model.Inscripcion;
import com.activetrack.inscripciones.model.Tarifa;
import com.activetrack.inscripciones.repository.AtletaRepository;
import com.activetrack.inscripciones.repository.CategoriaRepository;
import com.activetrack.inscripciones.repository.EventoRepository;
import com.activetrack.inscripciones.repository.InscripcionRepository;
import com.activetrack.inscripciones.repository.TarifaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Inscripción con alta concurrencia: el cupo se reserva de forma atómica en Redis y la solicitud
 * se responde de inmediato. El pago y el kit se resuelven después mediante eventos.
 */
@Service
public class InscripcionService {

    public record Solicitud(Long categoriaId, String nombres, String documento, LocalDate fechaNacimiento,
                            String genero, String email) {
    }

    public record Resultado(Long inscripcionId, String estado, BigDecimal monto, String tarifa,
                            Instant expiraEn, Long posicionEspera, String mensaje) {
    }

    private final CategoriaRepository categorias;
    private final EventoRepository eventos;
    private final TarifaRepository tarifas;
    private final AtletaRepository atletas;
    private final InscripcionRepository inscripciones;
    private final CupoService cupos;
    private final FichaService fichas;
    private final InscripcionEventPublisher publisher;
    private final Clock clock;
    private final Duration plazoPago;

    public InscripcionService(CategoriaRepository categorias, EventoRepository eventos, TarifaRepository tarifas,
                              AtletaRepository atletas, InscripcionRepository inscripciones, CupoService cupos,
                              FichaService fichas, InscripcionEventPublisher publisher, Clock clock,
                              @Value("${inscripciones.minutos-reserva}") long minutosReserva) {
        this.categorias = categorias;
        this.eventos = eventos;
        this.tarifas = tarifas;
        this.atletas = atletas;
        this.inscripciones = inscripciones;
        this.cupos = cupos;
        this.fichas = fichas;
        this.publisher = publisher;
        this.clock = clock;
        this.plazoPago = Duration.ofMinutes(minutosReserva);
    }

    @Transactional
    public Resultado inscribir(Solicitud s) {
        Categoria categoria = categorias.findById(s.categoriaId())
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada"));
        Evento evento = eventos.findById(categoria.getEventoId()).orElseThrow();
        if (!evento.inscripcionesAbiertas()) {
            throw new ReglaNegocioException("Las inscripciones de " + evento.getNombre() + " están cerradas");
        }
        Tarifa tarifa = ReglasInscripcion
                .tarifaVigente(tarifas.findByEventoIdOrderByVigenteDesde(evento.getId()), LocalDate.now(clock.withZone(Evento.ZONA)))
                .orElseThrow(() -> new ReglaNegocioException("No hay una tarifa vigente para hoy"));

        Atleta atleta = atletas.findByDocumento(s.documento())
                .map(a -> {
                    a.actualizarContacto(s.nombres(), s.email());
                    return a;
                })
                .orElseGet(() -> atletas.save(new Atleta(s.nombres(), s.documento(), s.fechaNacimiento(),
                        s.genero().toUpperCase(), s.email())));
        ReglasInscripcion.validarCategoria(categoria, atleta, evento.fechaLocal());
        if (inscripciones.tieneInscripcion(atleta.getId(), evento.getId(), EstadoInscripcion.ACTIVAS)) {
            throw new ReglaNegocioException("El atleta ya tiene una inscripción vigente en este evento");
        }

        Instant ahora = clock.instant();
        if (!cupos.reservar(categoria.getId())) {
            Inscripcion espera = inscripciones.save(
                    Inscripcion.enEspera(atleta.getId(), categoria.getId(), tarifa.getId(), ahora));
            Long posicion = fichas.posicionEspera(espera);
            return new Resultado(espera.getId(), espera.getEstado().name(), null, null, null, posicion,
                    "No quedan cupos: estás en el puesto " + posicion + " de la lista de espera");
        }

        Inscripcion reservada = inscripciones.save(
                Inscripcion.reservada(atleta.getId(), categoria.getId(), tarifa.getId(), ahora, ahora.plus(plazoPago)));
        publisher.reservada(new FichaInscripcion(reservada, atleta, categoria, evento, tarifa).eventoReservada());
        return new Resultado(reservada.getId(), reservada.getEstado().name(), tarifa.getPrecio(), tarifa.getNombre(),
                reservada.getExpiraEn(), null,
                "Cupo reservado: tienes " + plazoPago.toMinutes() + " minutos para pagar");
    }

    @Transactional(readOnly = true)
    public FichaInscripcion.Detalle detalle(Long inscripcionId, String documento) {
        FichaInscripcion ficha = fichas.delAtleta(inscripcionId, documento);
        return ficha.detalle(fichas.posicionEspera(ficha.inscripcion()));
    }

    @Transactional(readOnly = true)
    public List<FichaInscripcion.Detalle> delAtleta(String documento) {
        return atletas.findByDocumento(documento)
                .map(a -> inscripciones.findByAtletaIdOrderByCreadaEnDesc(a.getId()).stream()
                        .map(i -> fichas.de(i).detalle(fichas.posicionEspera(i)))
                        .toList())
                .orElse(List.of());
    }
}
