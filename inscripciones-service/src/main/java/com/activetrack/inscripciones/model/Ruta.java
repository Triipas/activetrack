package com.activetrack.inscripciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id")
    private Long eventoId;

    private String nombre;

    @Column(name = "distancia_km")
    private BigDecimal distanciaKm;

    protected Ruta() {
    }

    public Ruta(Long eventoId, String nombre, BigDecimal distanciaKm) {
        this.eventoId = eventoId;
        this.nombre = nombre;
        this.distanciaKm = distanciaKm;
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public String getNombre() { return nombre; }
    public BigDecimal getDistanciaKm() { return distanciaKm; }
}
