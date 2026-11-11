package com.activetrack.cronometraje.controller;

import com.activetrack.cronometraje.repository.EstadoAntenaView;
import com.activetrack.cronometraje.repository.LecturaRfidRepository;
import com.activetrack.cronometraje.service.LecturaInvalidaException;
import com.activetrack.cronometraje.service.LecturaService;
import com.activetrack.cronometraje.service.ResultadoLectura;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class LecturaController {

    /** Formato estándar de lectura que envían las antenas (o su software de integración). */
    public record LecturaRequest(@NotBlank String chipId, @NotBlank String antenaId, Instant marcaTiempo) {
    }

    private final LecturaService lecturaService;
    private final LecturaRfidRepository lecturaRepository;

    public LecturaController(LecturaService lecturaService, LecturaRfidRepository lecturaRepository) {
        this.lecturaService = lecturaService;
        this.lecturaRepository = lecturaRepository;
    }

    /** Responde 202 Accepted: la lectura quedó registrada y el resto ocurre de forma asíncrona. */
    @PostMapping("/api/lecturas")
    public ResponseEntity<ResultadoLectura> registrar(@Valid @RequestBody LecturaRequest request) {
        ResultadoLectura resultado = lecturaService.registrar(request.chipId(), request.antenaId(), request.marcaTiempo());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resultado);
    }

    /** Monitoreo de conectividad de antenas para el cronometrador. */
    @GetMapping("/api/antenas")
    public List<EstadoAntenaView> estadoAntenas(@RequestParam(defaultValue = "1") Long eventoId) {
        return lecturaRepository.estadoAntenas(eventoId);
    }

    /** Solo para la demo: reinicia las lecturas del evento. */
    @DeleteMapping("/api/lecturas")
    public Map<String, Integer> reiniciar(@RequestParam(defaultValue = "1") Long eventoId) {
        return Map.of("lecturasBorradas", lecturaRepository.borrarPorEvento(eventoId));
    }

    @ExceptionHandler(LecturaInvalidaException.class)
    public ResponseEntity<Map<String, String>> lecturaInvalida(LecturaInvalidaException e) {
        return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
    }
}
