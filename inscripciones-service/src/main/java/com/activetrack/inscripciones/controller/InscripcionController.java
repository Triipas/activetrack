package com.activetrack.inscripciones.controller;

import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.service.CatalogoService;
import com.activetrack.inscripciones.service.CatalogoService.EventoCatalogo;
import com.activetrack.inscripciones.service.FichaInscripcion;
import com.activetrack.inscripciones.service.InscripcionService;
import com.activetrack.inscripciones.service.PagoService;
import com.activetrack.inscripciones.service.PagoService.CobroIniciado;
import com.activetrack.inscripciones.service.PagoService.InfoPago;
import com.activetrack.inscripciones.service.QrService;
import com.activetrack.inscripciones.service.ReglaNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** API pública para atletas: catálogo, inscripción, pago y kit digital. */
@RestController
public class InscripcionController {

    public record SolicitudRequest(
            @NotNull Long categoriaId,
            @NotBlank String nombres,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{6,20}", message = "Documento inválido") String documento,
            @NotNull @Past LocalDate fechaNacimiento,
            @NotBlank @Pattern(regexp = "[MFmf]", message = "Use M o F") String genero,
            @NotBlank @Email String email) {
    }

    public record NotificacionPago(@NotBlank String referencia, @NotBlank String estado) {
    }

    private final CatalogoService catalogo;
    private final InscripcionService inscripciones;
    private final PagoService pagos;
    private final QrService qr;

    public InscripcionController(CatalogoService catalogo, InscripcionService inscripciones, PagoService pagos,
                                 QrService qr) {
        this.catalogo = catalogo;
        this.inscripciones = inscripciones;
        this.pagos = pagos;
        this.qr = qr;
    }

    @GetMapping("/api/catalogo/eventos")
    public List<EventoCatalogo> eventosAbiertos() {
        return catalogo.eventosAbiertos();
    }

    /** 202 Accepted: el cupo quedó reservado (o en espera) y el pago se completa después. */
    @PostMapping("/api/inscripciones")
    public ResponseEntity<InscripcionService.Resultado> inscribir(@Valid @RequestBody SolicitudRequest r) {
        InscripcionService.Resultado resultado = inscripciones.inscribir(new InscripcionService.Solicitud(
                r.categoriaId(), r.nombres().trim(), r.documento().trim(), r.fechaNacimiento(), r.genero(),
                r.email().trim()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resultado);
    }

    @GetMapping("/api/inscripciones")
    public List<FichaInscripcion.Detalle> delAtleta(@RequestParam String documento) {
        return inscripciones.delAtleta(documento);
    }

    @GetMapping("/api/inscripciones/{id}")
    public FichaInscripcion.Detalle detalle(@PathVariable Long id, @RequestParam String documento) {
        return inscripciones.detalle(id, documento);
    }

    @PostMapping("/api/inscripciones/{id}/pago")
    public CobroIniciado iniciarPago(@PathVariable Long id, @RequestParam String documento) {
        return pagos.iniciar(id, documento);
    }

    /** Kit digital: código QR que se presenta para recoger el dorsal y el chip. */
    @GetMapping(value = "/api/inscripciones/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable Long id, @RequestParam String documento) {
        FichaInscripcion.Detalle d = inscripciones.detalle(id, documento);
        if (!EstadoInscripcion.CONFIRMADA.name().equals(d.estado())) {
            throw new ReglaNegocioException("El kit está disponible cuando se confirma el pago");
        }
        String contenido = String.join("|", "ACTIVETRACK", d.codigoQr(), String.valueOf(d.dorsal()), d.chipId(),
                d.documento());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(qr.png(contenido));
    }

    @GetMapping("/api/pagos/{referencia}")
    public InfoPago infoPago(@PathVariable String referencia) {
        return pagos.info(referencia);
    }

    /** Webhook que llama la pasarela de pagos al aprobar o rechazar el cobro. */
    @PostMapping("/api/pagos/webhook")
    public InfoPago webhook(@Valid @RequestBody NotificacionPago n) {
        return pagos.procesarNotificacion(n.referencia(), "APROBADO".equalsIgnoreCase(n.estado()));
    }
}
