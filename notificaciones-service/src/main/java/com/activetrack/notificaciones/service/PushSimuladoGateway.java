package com.activetrack.notificaciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Implementación para la PoC: registra los push en el log y guarda los últimos para mostrarlos en la web.
 * En producción se reemplaza por una implementación que llame a Firebase Cloud Messaging.
 */
@Component
public class PushSimuladoGateway implements PushGateway {

    public record PushEnviado(Instant fecha, String token, String titulo, String cuerpo) {
    }

    private static final Logger log = LoggerFactory.getLogger(PushSimuladoGateway.class);
    private static final int MAXIMO = 50;

    private final Deque<PushEnviado> historial = new ConcurrentLinkedDeque<>();

    @Override
    public void enviar(String tokenDispositivo, String titulo, String cuerpo) {
        log.info("PUSH -> {} | {} | {}", tokenDispositivo, titulo, cuerpo);
        historial.addFirst(new PushEnviado(Instant.now(), tokenDispositivo, titulo, cuerpo));
        while (historial.size() > MAXIMO) {
            historial.pollLast();
        }
    }

    public List<PushEnviado> historial() {
        return new ArrayList<>(historial);
    }
}
