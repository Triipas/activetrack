package com.activetrack.inscripciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pago")
public class Pago {

    public enum Estado { PENDIENTE, APROBADO, RECHAZADO, REEMBOLSADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inscripcion_id")
    private Long inscripcionId;

    private BigDecimal monto;

    @Column(name = "referencia_pasarela")
    private String referenciaPasarela;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    private Instant fecha;

    protected Pago() {
    }

    public Pago(Long inscripcionId, BigDecimal monto, String referenciaPasarela, Instant fecha) {
        this.inscripcionId = inscripcionId;
        this.monto = monto;
        this.referenciaPasarela = referenciaPasarela;
        this.estado = Estado.PENDIENTE;
        this.fecha = fecha;
    }

    public void aprobar(Instant ahora) { cambiar(Estado.PENDIENTE, Estado.APROBADO, ahora); }

    public void rechazar(Instant ahora) { cambiar(Estado.PENDIENTE, Estado.RECHAZADO, ahora); }

    public void reembolsar(Instant ahora) { cambiar(Estado.APROBADO, Estado.REEMBOLSADO, ahora); }

    private void cambiar(Estado desde, Estado hacia, Instant ahora) {
        if (estado != desde) {
            throw new IllegalStateException("El pago está " + estado);
        }
        this.estado = hacia;
        this.fecha = ahora;
    }

    public Long getId() { return id; }
    public Long getInscripcionId() { return inscripcionId; }
    public BigDecimal getMonto() { return monto; }
    public String getReferenciaPasarela() { return referenciaPasarela; }
    public Estado getEstado() { return estado; }
    public Instant getFecha() { return fecha; }
}
