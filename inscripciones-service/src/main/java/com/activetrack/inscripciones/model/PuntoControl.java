package com.activetrack.inscripciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "punto_control")
public class PuntoControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ruta_id")
    private Long rutaId;

    private String nombre;
    private BigDecimal km;
    private int orden;

    @Column(name = "es_meta")
    private boolean esMeta;

    @Column(name = "antena_id")
    private String antenaId;

    protected PuntoControl() {
    }

    public PuntoControl(Long rutaId, String nombre, BigDecimal km, int orden, boolean esMeta, String antenaId) {
        this.rutaId = rutaId;
        this.nombre = nombre;
        this.km = km;
        this.orden = orden;
        this.esMeta = esMeta;
        this.antenaId = antenaId;
    }

    public Long getId() { return id; }
    public Long getRutaId() { return rutaId; }
    public String getNombre() { return nombre; }
    public BigDecimal getKm() { return km; }
    public int getOrden() { return orden; }
    public boolean isEsMeta() { return esMeta; }
    public String getAntenaId() { return antenaId; }
}
