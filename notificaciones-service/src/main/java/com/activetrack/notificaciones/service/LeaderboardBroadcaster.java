package com.activetrack.notificaciones.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Avisa a las pantallas del leaderboard que hubo un cambio, para que lo vuelvan a cargar.
 * Así la tabla se actualiza sin que el navegador consulte cada segundo (sin polling).
 */
@Component
public class LeaderboardBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardBroadcaster.class);

    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public LeaderboardBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registrar(WebSocketSession sesion) {
        sesiones.put(sesion.getId(), sesion);
    }

    public void quitar(WebSocketSession sesion) {
        sesiones.remove(sesion.getId());
    }

    public void notificarCambio(Long eventoId, Long inscripcionId, Instant recibidoEn) {
        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of(
                    "tipo", "LEADERBOARD", "eventoId", eventoId, "inscripcionId", inscripcionId,
                    "recibidoEn", recibidoEn));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        sesiones.values().removeIf(s -> !s.isOpen());
        for (WebSocketSession sesion : sesiones.values()) {
            try {
                sesion.sendMessage(new TextMessage(json));
            } catch (IOException | IllegalStateException e) {
                log.warn("No se pudo avisar a la sesión {}: {}", sesion.getId(), e.getMessage());
            }
        }
    }
}
