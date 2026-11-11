package com.activetrack.notificaciones.messaging;

import com.activetrack.events.Colas;
import com.activetrack.events.InscripcionConfirmada;
import com.activetrack.events.InscripcionReservada;
import com.activetrack.notificaciones.service.CorreoGateway;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Consume los eventos de inscripción y avisa al atleta por correo. */
@Component
@RabbitListener(queues = Colas.COLA_NOTIFICACIONES_INSCRIPCIONES)
public class InscripcionEventListener {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Lima"));

    private final CorreoGateway correo;

    public InscripcionEventListener(CorreoGateway correo) {
        this.correo = correo;
    }

    @RabbitHandler
    public void onReservada(InscripcionReservada e) {
        correo.enviar(e.email(), "Tu cupo en " + e.evento() + " está reservado",
                "Hola " + e.atleta() + ", reservamos tu cupo en la categoría " + e.categoria()
                        + ". Completa el pago de S/ " + e.monto() + " antes de las " + HORA.format(e.expiraEn())
                        + " (inscripción #" + e.inscripcionId() + ").");
    }

    @RabbitHandler
    public void onConfirmada(InscripcionConfirmada e) {
        correo.enviar(e.email(), "Inscripción confirmada: dorsal " + e.dorsal(),
                "Hola " + e.atleta() + ", tu inscripción en " + e.evento() + " (" + e.categoria()
                        + ") está confirmada. Dorsal " + e.dorsal() + ", chip " + e.chipId()
                        + ". Descarga tu kit digital con el código " + e.codigoQr() + ".");
    }
}
