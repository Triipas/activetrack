package com.activetrack.cronometraje.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "punto_control")
public class PuntoControl {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    private String nombre;
    private BigDecimal km;
    private int orden;

    @Column(name = "es_meta")
    private boolean esMeta;

    @Column(name = "antena_id")
    private String antenaId;

    protected PuntoControl() {
    }

    public PuntoControl(Long id, Ruta ruta, String nombre, BigDecimal km, int orden, boolean esMeta, String antenaId) {
        this.id = id;
        this.ruta = ruta;
        this.nombre = nombre;
        this.km = km;
        this.orden = orden;
        this.esMeta = esMeta;
        this.antenaId = antenaId;
    }

    public Long getId() { return id; }
    public Ruta getRuta() { return ruta; }
    public String getNombre() { return nombre; }
    public BigDecimal getKm() { return km; }
    public int getOrden() { return orden; }
    public boolean isEsMeta() { return esMeta; }
    public String getAntenaId() { return antenaId; }
}
