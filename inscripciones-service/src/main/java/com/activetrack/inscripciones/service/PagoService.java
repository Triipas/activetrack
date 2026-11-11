package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.messaging.InscripcionEventPublisher;
import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.model.Inscripcion;
import com.activetrack.inscripciones.model.Pago;
import com.activetrack.inscripciones.repository.InscripcionRepository;
import com.activetrack.inscripciones.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/** Cobro de la inscripción con la pasarela externa, confirmación por webhook y reembolsos. */
@Service
public class PagoService {

    public record CobroIniciado(String referencia, BigDecimal monto, String urlCheckout) {
    }

    public record InfoPago(String referencia, BigDecimal monto, String descripcion, String estado) {
    }

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PagoRepository pagos;
    private final InscripcionRepository inscripciones;
    private final FichaService fichas;
    private final PasarelaPagos pasarela;
    private final CupoService cupos;
    private final ListaEsperaService listaEspera;
    private final InscripcionEventPublisher publisher;
    private final Clock clock;

    public PagoService(PagoRepository pagos, InscripcionRepository inscripciones, FichaService fichas,
                       PasarelaPagos pasarela, CupoService cupos, ListaEsperaService listaEspera,
                       InscripcionEventPublisher publisher, Clock clock) {
        this.pagos = pagos;
        this.inscripciones = inscripciones;
        this.fichas = fichas;
        this.pasarela = pasarela;
        this.cupos = cupos;
        this.listaEspera = listaEspera;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public CobroIniciado iniciar(Long inscripcionId, String documento) {
        FichaInscripcion ficha = fichas.delAtleta(inscripcionId, documento);
        Inscripcion inscripcion = ficha.inscripcion();
        if (inscripcion.getEstado() != EstadoInscripcion.PENDIENTE_PAGO) {
            throw new ReglaNegocioException("La inscripción no está pendiente de pago (estado " + inscripcion.getEstado() + ")");
        }
        if (inscripcion.vencida(clock.instant())) {
            throw new ReglaNegocioException("El plazo para pagar venció");
        }
        Pago pago = pagos.findFirstByInscripcionIdAndEstado(inscripcionId, Pago.Estado.PENDIENTE)
                .orElseGet(() -> pagos.save(new Pago(inscripcionId, ficha.tarifa().getPrecio(),
                        "AT-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(), clock.instant())));
        String url = pasarela.crearCobro(pago.getReferenciaPasarela(), pago.getMonto(), descripcion(ficha));
        return new CobroIniciado(pago.getReferenciaPasarela(), pago.getMonto(), url);
    }

    @Transactional(readOnly = true)
    public InfoPago info(String referencia) {
        Pago pago = buscar(referencia);
        FichaInscripcion ficha = fichas.de(inscripciones.findById(pago.getInscripcionId()).orElseThrow());
        return new InfoPago(referencia, pago.getMonto(), descripcion(ficha), pago.getEstado().name());
    }

    /** Notificación de la pasarela. Es idempotente: si llega dos veces, la segunda no cambia nada. */
    @Transactional
    public InfoPago procesarNotificacion(String referencia, boolean aprobado) {
        Pago pago = buscar(referencia);
        if (pago.getEstado() != Pago.Estado.PENDIENTE) {
            return info(referencia);
        }
        Instant ahora = clock.instant();
        Inscripcion inscripcion = inscripciones.findById(pago.getInscripcionId()).orElseThrow();
        if (!aprobado) {
            pago.rechazar(ahora);
        } else {
            pago.aprobar(ahora);
            if (inscripcion.getEstado() == EstadoInscripcion.PENDIENTE_PAGO) {
                int dorsal = inscripciones.siguienteDorsal();
                inscripcion.confirmar(dorsal, "CHIP-" + dorsal, "AT-" + inscripcion.getId() + "-" + codigoAleatorio());
                publisher.confirmada(fichas.de(inscripcion).eventoConfirmada());
            } else {
                // El pago llegó cuando la reserva ya había expirado: se devuelve el dinero.
                log.warn("Pago {} recibido para la inscripción {} en estado {}: se reembolsa",
                        referencia, inscripcion.getId(), inscripcion.getEstado());
                pasarela.reembolsar(referencia, pago.getMonto());
                pago.reembolsar(ahora);
            }
        }
        return new InfoPago(referencia, pago.getMonto(), descripcion(fichas.de(inscripcion)), pago.getEstado().name());
    }

    /** Reembolso solicitado por el organizador: anula la inscripción y libera el cupo. */
    @Transactional
    public void reembolsar(Long inscripcionId) {
        Inscripcion inscripcion = inscripciones.findById(inscripcionId)
                .orElseThrow(() -> new NoEncontradoException("Inscripción no encontrada"));
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA) {
            throw new ReglaNegocioException("Solo se reembolsan inscripciones confirmadas");
        }
        Pago pago = pagos.findFirstByInscripcionIdAndEstado(inscripcionId, Pago.Estado.APROBADO)
                .orElseThrow(() -> new ReglaNegocioException("La inscripción no tiene un pago aprobado"));
        pasarela.reembolsar(pago.getReferenciaPasarela(), pago.getMonto());
        pago.reembolsar(clock.instant());
        inscripcion.anular();
        cupos.liberar(inscripcion.getCategoriaId());
        listaEspera.promover(inscripcion.getCategoriaId());
    }

    private Pago buscar(String referencia) {
        return pagos.findByReferenciaPasarela(referencia)
                .orElseThrow(() -> new NoEncontradoException("Pago no encontrado"));
    }

    private static String descripcion(FichaInscripcion f) {
        return f.evento().getNombre() + " · " + f.categoria().getNombre() + " · " + f.atleta().getNombres();
    }

    private static String codigoAleatorio() {
        byte[] bytes = new byte[4];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }
}
