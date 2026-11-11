package com.activetrack.notificaciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Implementación para la PoC: registra los correos en el log y guarda los últimos para verlos en la web. */
@Component
public class CorreoSimuladoGateway implements CorreoGateway {

    public record CorreoEnviado(Instant fecha, String destinatario, String asunto, String cuerpo) {
    }

    private static final Logger log = LoggerFactory.getLogger(CorreoSimuladoGateway.class);
    private static final int MAXIMO = 50;

    private final Deque<CorreoEnviado> historial = new ConcurrentLinkedDeque<>();

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        log.info("CORREO -> {} | {}", destinatario, asunto);
        historial.addFirst(new CorreoEnviado(Instant.now(), destinatario, asunto, cuerpo));
        while (historial.size() > MAXIMO) {
            historial.pollLast();
        }
    }

    public List<CorreoEnviado> historial() {
        return new ArrayList<>(historial);
    }
}
