package com.activetrack.notificaciones.controller;

import com.activetrack.notificaciones.observer.WebSocketSeguidor;
import com.activetrack.notificaciones.service.GestorSeguimientos;
import com.activetrack.notificaciones.service.LeaderboardBroadcaster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal WebSocket del espectador. Mensajes que acepta:
 * <pre>
 * {"accion":"seguir","atletaId":7}       -> se suscribe a los cruces del atleta
 * {"accion":"dejar","atletaId":7}        -> deja de seguirlo
 * {"accion":"leaderboard"}               -> recibe avisos de cambio del leaderboard
 * </pre>
 */
@Component
public class SeguimientoWebSocketHandler extends TextWebSocketHandler {

    private static final int TIMEOUT_ENVIO_MS = 5_000;
    private static final int BUFFER_BYTES = 512 * 1024;

    private final GestorSeguimientos gestor;
    private final LeaderboardBroadcaster broadcaster;
    private final ObjectMapper objectMapper;
    /** Sesiones envueltas para que varios hilos puedan enviar mensajes de forma segura. */
    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();

    public SeguimientoWebSocketHandler(GestorSeguimientos gestor, LeaderboardBroadcaster broadcaster,
                                       ObjectMapper objectMapper) {
        this.gestor = gestor;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sesiones.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, TIMEOUT_ENVIO_MS, BUFFER_BYTES));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        WebSocketSession segura = sesiones.get(session.getId());
        JsonNode json = objectMapper.readTree(message.getPayload());
        String accion = json.path("accion").asText();
        switch (accion) {
            case "seguir" -> gestor.seguir(json.path("atletaId").asLong(), new WebSocketSeguidor(segura, objectMapper));
            case "dejar" -> gestor.dejarDeSeguir(json.path("atletaId").asLong(), "ws:" + session.getId());
            case "leaderboard" -> broadcaster.registrar(segura);
            default -> segura.sendMessage(new TextMessage("{\"tipo\":\"ERROR\",\"mensaje\":\"Acción desconocida\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketSession segura = sesiones.remove(session.getId());
        gestor.quitarSeguidor("ws:" + session.getId());
        if (segura != null) {
            broadcaster.quitar(segura);
        }
    }
}
