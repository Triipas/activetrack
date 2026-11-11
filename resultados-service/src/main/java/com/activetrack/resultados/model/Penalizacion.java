package com.activetrack.resultados.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "penalizacion")
public class Penalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resultado_id")
    private Long resultadoId;

    @Enumerated(EnumType.STRING)
    private TipoPenalizacion tipo;

    private int segundos;
    private String motivo;
    private String juez;
    private Instant fecha;

    protected Penalizacion() {
    }

    public Penalizacion(Long resultadoId, TipoPenalizacion tipo, int segundos, String motivo, String juez, Instant fecha) {
        this.resultadoId = resultadoId;
        this.tipo = tipo;
        this.segundos = segundos;
        this.motivo = motivo;
        this.juez = juez;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getResultadoId() { return resultadoId; }
    public TipoPenalizacion getTipo() { return tipo; }
    public int getSegundos() { return segundos; }
    public String getMotivo() { return motivo; }
    public String getJuez() { return juez; }
    public Instant getFecha() { return fecha; }
}
