package com.activetrack.inscripciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Inscripción de un atleta en una categoría. Las transiciones de estado se controlan aquí. */
@Entity
@Table(name = "inscripcion")
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atleta_id")
    private Long atletaId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(name = "tarifa_id")
    private Long tarifaId;

    private Integer dorsal;

    @Column(name = "chip_id")
    private String chipId;

    @Enumerated(EnumType.STRING)
    private EstadoInscripcion estado;

    @Column(name = "codigo_qr")
    private String codigoQr;

    @Column(name = "expira_en")
    private Instant expiraEn;

    @Column(name = "creada_en")
    private Instant creadaEn;

    protected Inscripcion() {
    }

    private Inscripcion(Long atletaId, Long categoriaId, Long tarifaId, EstadoInscripcion estado,
                        Instant creadaEn, Instant expiraEn) {
        this.atletaId = atletaId;
        this.categoriaId = categoriaId;
        this.tarifaId = tarifaId;
        this.estado = estado;
        this.creadaEn = creadaEn;
        this.expiraEn = expiraEn;
    }

    public static Inscripcion reservada(Long atletaId, Long categoriaId, Long tarifaId, Instant ahora, Instant expiraEn) {
        return new Inscripcion(atletaId, categoriaId, tarifaId, EstadoInscripcion.PENDIENTE_PAGO, ahora, expiraEn);
    }

    public static Inscripcion enEspera(Long atletaId, Long categoriaId, Long tarifaId, Instant ahora) {
        return new Inscripcion(atletaId, categoriaId, tarifaId, EstadoInscripcion.EN_ESPERA, ahora, null);
    }

    public boolean vencida(Instant ahora) {
        return estado == EstadoInscripcion.PENDIENTE_PAGO && expiraEn != null && !ahora.isBefore(expiraEn);
    }

    /** Se liberó un cupo: el primero de la lista de espera pasa a tener plazo para pagar. */
    public void promoverDesdeEspera(Instant expira) {
        exigir(EstadoInscripcion.EN_ESPERA);
        this.estado = EstadoInscripcion.PENDIENTE_PAGO;
        this.expiraEn = expira;
    }

    public void confirmar(int dorsal, String chipId, String codigoQr) {
        exigir(EstadoInscripcion.PENDIENTE_PAGO);
        this.estado = EstadoInscripcion.CONFIRMADA;
        this.dorsal = dorsal;
        this.chipId = chipId;
        this.codigoQr = codigoQr;
        this.expiraEn = null;
    }

    public void expirar() {
        exigir(EstadoInscripcion.PENDIENTE_PAGO);
        this.estado = EstadoInscripcion.EXPIRADA;
    }

    public void anular() {
        exigir(EstadoInscripcion.CONFIRMADA);
        this.estado = EstadoInscripcion.ANULADA;
    }

    private void exigir(EstadoInscripcion esperado) {
        if (estado != esperado) {
            throw new IllegalStateException("La inscripción está " + estado + " y se esperaba " + esperado);
        }
    }

    public Long getId() { return id; }
    public Long getAtletaId() { return atletaId; }
    public Long getCategoriaId() { return categoriaId; }
    public Long getTarifaId() { return tarifaId; }
    public Integer getDorsal() { return dorsal; }
    public String getChipId() { return chipId; }
    public EstadoInscripcion getEstado() { return estado; }
    public String getCodigoQr() { return codigoQr; }
    public Instant getExpiraEn() { return expiraEn; }
    public Instant getCreadaEn() { return creadaEn; }
}
