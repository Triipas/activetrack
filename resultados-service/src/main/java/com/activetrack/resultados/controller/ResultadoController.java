package com.activetrack.resultados.controller;

import com.activetrack.events.ResultadoActualizado;
import com.activetrack.resultados.model.TipoPenalizacion;
import com.activetrack.resultados.repository.ConsultaRepository;
import com.activetrack.resultados.repository.ConsultaRepository.AtletaResumen;
import com.activetrack.resultados.repository.ConsultaRepository.CategoriaResumen;
import com.activetrack.resultados.repository.ConsultaRepository.EventoResumen;
import com.activetrack.resultados.service.DisputaService;
import com.activetrack.resultados.service.DisputaService.Anomalia;
import com.activetrack.resultados.service.DisputaService.DecisionJuez;
import com.activetrack.resultados.service.LeaderboardService;
import com.activetrack.resultados.service.LeaderboardService.LeaderboardFila;
import com.activetrack.resultados.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
public class ResultadoController {

    public record PenalizacionRequest(@NotNull TipoPenalizacion tipo, @Min(0) int segundos,
                                      @NotBlank String motivo, @NotBlank String juez) {
    }

    private final LeaderboardService leaderboard;
    private final DisputaService disputas;
    private final ConsultaRepository consultas;
    private final ReporteService reportes;

    public ResultadoController(LeaderboardService leaderboard, DisputaService disputas, ConsultaRepository consultas,
                               ReporteService reportes) {
        this.leaderboard = leaderboard;
        this.disputas = disputas;
        this.consultas = consultas;
        this.reportes = reportes;
    }

    @GetMapping("/api/eventos/{eventoId}")
    public EventoResumen evento(@PathVariable Long eventoId) {
        return consultas.evento(eventoId).orElseThrow(() -> new NoSuchElementException("Evento no encontrado"));
    }

    @GetMapping("/api/eventos/{eventoId}/leaderboard")
    public List<LeaderboardFila> leaderboard(@PathVariable Long eventoId,
                                             @RequestParam(required = false) Long categoriaId,
                                             @RequestParam(defaultValue = "50") int limite) {
        return leaderboard.consultar(eventoId, categoriaId, limite);
    }

    @GetMapping("/api/eventos/{eventoId}/categorias")
    public List<CategoriaResumen> categorias(@PathVariable Long eventoId) {
        return consultas.categorias(eventoId);
    }

    @GetMapping("/api/eventos/{eventoId}/atletas")
    public List<AtletaResumen> atletas(@PathVariable Long eventoId) {
        return consultas.atletas(eventoId);
    }

    @GetMapping("/api/eventos/{eventoId}/anomalias")
    public List<Anomalia> anomalias(@PathVariable Long eventoId) {
        return disputas.anomalias(eventoId);
    }

    @PostMapping("/api/resultados/{inscripcionId}/penalizaciones")
    public ResultadoActualizado penalizar(@PathVariable Long inscripcionId,
                                          @Valid @RequestBody PenalizacionRequest request) {
        return disputas.aplicarDecision(inscripcionId,
                new DecisionJuez(request.tipo(), request.segundos(), request.motivo(), request.juez()));
    }

    /** Historial del atleta: resultados de todos sus eventos, posiciones y ritmo. */
    @GetMapping("/api/resultados/atletas/{documento}/historial")
    public ReporteService.Historial historial(@PathVariable String documento) {
        return reportes.historial(documento);
    }

    @GetMapping("/api/eventos/{eventoId}/resultados.csv")
    public ResponseEntity<byte[]> resultadosCsv(@PathVariable Long eventoId) {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] datos = reportes.resultadosCsv(eventoId).getBytes(StandardCharsets.UTF_8);
        byte[] contenido = new byte[bom.length + datos.length];
        System.arraycopy(bom, 0, contenido, 0, bom.length);
        System.arraycopy(datos, 0, contenido, bom.length, datos.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resultados-evento-" + eventoId + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido);
    }

    /** Solo para la demo: borra resultados, penalizaciones y el leaderboard del evento. */
    @DeleteMapping("/api/eventos/{eventoId}/resultados")
    @Transactional
    public Map<String, String> reiniciar(@PathVariable Long eventoId) {
        leaderboard.borrarEvento(eventoId, consultas.categoriasDeEvento(eventoId),
                consultas.inscripcionesDeEvento(eventoId));
        consultas.borrarResultados(eventoId);
        return Map.of("estado", "REINICIADO");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> invalido(RuntimeException e) {
        return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
    }
}
