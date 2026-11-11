package com.activetrack.inscripciones.controller;

import com.activetrack.inscripciones.model.Evento;
import com.activetrack.inscripciones.model.Tarifa;
import com.activetrack.inscripciones.repository.InscripcionRepository.InscripcionAdminView;
import com.activetrack.inscripciones.service.EventoAdminService;
import com.activetrack.inscripciones.service.EventoAdminService.NuevaCategoria;
import com.activetrack.inscripciones.service.EventoAdminService.NuevaTarifa;
import com.activetrack.inscripciones.service.EventoAdminService.NuevoEvento;
import com.activetrack.inscripciones.service.EventoAdminService.NuevoPunto;
import com.activetrack.inscripciones.service.EventoAdminService.ResumenEvento;
import com.activetrack.inscripciones.service.PagoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** API del organizador (requiere rol ORGANIZADOR). */
@RestController
public class AdminController {

    public record EventoRequest(@NotBlank String nombre, @NotBlank String tipo, @NotNull Instant fecha,
                                @NotBlank String ruta, @NotNull @DecimalMin("0.1") BigDecimal distanciaKm,
                                @NotEmpty List<@Valid PuntoRequest> puntos,
                                @NotEmpty List<@Valid CategoriaRequest> categorias,
                                @NotEmpty List<@Valid TarifaRequest> tarifas) {
    }

    public record PuntoRequest(@NotBlank String nombre, @NotNull @DecimalMin("0") BigDecimal km,
                               @NotBlank String antenaId) {
    }

    public record CategoriaRequest(@NotBlank String nombre, @Min(0) int edadMin, @Min(0) int edadMax,
                                   @NotBlank @Pattern(regexp = "[MFXmfx]") String genero, @Min(1) int cupoMaximo) {
    }

    public record TarifaRequest(@NotBlank String nombre, @NotNull @DecimalMin("0") BigDecimal precio,
                                @NotNull LocalDate vigenteDesde, @NotNull LocalDate vigenteHasta) {
    }

    public record CupoRequest(@Min(0) int cupoMaximo) {
    }

    public record EstadoRequest(@NotBlank String estado) {
    }

    private final EventoAdminService admin;
    private final PagoService pagos;

    public AdminController(EventoAdminService admin, PagoService pagos) {
        this.admin = admin;
        this.pagos = pagos;
    }

    @PostMapping("/api/admin/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumenEvento crear(@Valid @RequestBody EventoRequest r) {
        return admin.crear(new NuevoEvento(r.nombre(), r.tipo(), r.fecha(), r.ruta(), r.distanciaKm(),
                r.puntos().stream().map(p -> new NuevoPunto(p.nombre(), p.km(), p.antenaId())).toList(),
                r.categorias().stream().map(c -> new NuevaCategoria(c.nombre(), c.edadMin(), c.edadMax(),
                        c.genero(), c.cupoMaximo())).toList(),
                r.tarifas().stream().map(AdminController::tarifa).toList()));
    }

    @GetMapping("/api/admin/eventos")
    public List<Evento> eventos() {
        return admin.listar();
    }

    @GetMapping("/api/admin/eventos/{id}")
    public ResumenEvento resumen(@PathVariable Long id) {
        return admin.resumen(id);
    }

    @PutMapping("/api/admin/eventos/{id}/estado")
    public ResumenEvento cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest r) {
        admin.cambiarEstado(id, r.estado());
        return admin.resumen(id);
    }

    @PostMapping("/api/admin/eventos/{id}/tarifas")
    @ResponseStatus(HttpStatus.CREATED)
    public Tarifa agregarTarifa(@PathVariable Long id, @Valid @RequestBody TarifaRequest r) {
        return admin.agregarTarifa(id, tarifa(r));
    }

    @PutMapping("/api/admin/categorias/{id}/cupo")
    public void cambiarCupo(@PathVariable Long id, @Valid @RequestBody CupoRequest r) {
        admin.cambiarCupo(id, r.cupoMaximo());
    }

    @GetMapping("/api/admin/eventos/{id}/inscripciones")
    public List<InscripcionAdminView> inscripciones(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "100") int limite) {
        return admin.inscripciones(id, Math.min(limite, 1000));
    }

    @PostMapping("/api/admin/inscripciones/{id}/reembolso")
    public void reembolsar(@PathVariable Long id) {
        pagos.reembolsar(id);
    }

    @GetMapping("/api/admin/eventos/{id}/inscritos.csv")
    public ResponseEntity<byte[]> exportar(@PathVariable Long id) {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}; // para que Excel reconozca UTF-8
        byte[] datos = admin.exportarCsv(id).getBytes(StandardCharsets.UTF_8);
        byte[] contenido = new byte[bom.length + datos.length];
        System.arraycopy(bom, 0, contenido, 0, bom.length);
        System.arraycopy(datos, 0, contenido, bom.length, datos.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inscritos-evento-" + id + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido);
    }

    private static NuevaTarifa tarifa(TarifaRequest t) {
        return new NuevaTarifa(t.nombre(), t.precio(), t.vigenteDesde(), t.vigenteHasta());
    }
}
