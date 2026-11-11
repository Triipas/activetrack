package com.activetrack.notificaciones.observer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Patrón Observer — rol <b>ConcreteObserver</b>.
 * Envía el cruce al navegador del espectador por WebSocket.
 */
public class WebSocketSeguidor implements SeguidorAtleta {

    private final WebSocketSession sesion;
    private final ObjectMapper objectMapper;

    public WebSocketSeguidor(WebSocketSession sesion, ObjectMapper objectMapper) {
        this.sesion = sesion;
        this.objectMapper = objectMapper;
    }

    @Override
    public void actualizar(CruceDTO cruce) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("tipo", "CRUCE", "cruce", cruce));
            sesion.sendMessage(new TextMessage(json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String id() {
        return "ws:" + sesion.getId();
    }

    @Override
    public boolean activo() {
        return sesion.isOpen();
    }
}
