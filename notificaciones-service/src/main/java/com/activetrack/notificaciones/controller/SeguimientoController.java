package com.activetrack.notificaciones.controller;

import com.activetrack.notificaciones.observer.PushSeguidor;
import com.activetrack.notificaciones.service.CorreoSimuladoGateway;
import com.activetrack.notificaciones.service.GestorSeguimientos;
import com.activetrack.notificaciones.service.PushSimuladoGateway;
import com.activetrack.notificaciones.service.PushSimuladoGateway.PushEnviado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SeguimientoController {

    public record SeguimientoPushRequest(@NotNull Long atletaId, @NotBlank String tokenDispositivo) {
    }

    private final GestorSeguimientos gestor;
    private final PushSimuladoGateway pushGateway;
    private final CorreoSimuladoGateway correoGateway;

    public SeguimientoController(GestorSeguimientos gestor, PushSimuladoGateway pushGateway,
                                 CorreoSimuladoGateway correoGateway) {
        this.gestor = gestor;
        this.pushGateway = pushGateway;
        this.correoGateway = correoGateway;
    }

    /** Suscribe un dispositivo móvil (token push) a los cruces de un atleta. */
    @PostMapping("/api/seguimientos/push")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> seguirPorPush(@Valid @RequestBody SeguimientoPushRequest request) {
        gestor.seguir(request.atletaId(), new PushSeguidor(request.tokenDispositivo(), pushGateway));
        return Map.of("atletaId", request.atletaId(), "canal", "PUSH");
    }

    /** Cantidad de seguidores por atleta. */
    @GetMapping("/api/seguimientos")
    public Map<Long, Integer> estadisticas() {
        return gestor.estadisticas();
    }

    /** Últimos correos enviados a los atletas (simulados en la PoC). */
    @GetMapping("/api/correos")
    public List<CorreoSimuladoGateway.CorreoEnviado> correos() {
        return correoGateway.historial();
    }

    /** Últimas notificaciones push enviadas (simuladas en la PoC). */
    @GetMapping("/api/seguimientos/push")
    public List<PushEnviado> historialPush() {
        return pushGateway.historial();
    }
}
