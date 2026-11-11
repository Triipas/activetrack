package com.activetrack.cronometraje.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    private Long id;

    @Column(name = "evento_id")
    private Long eventoId;

    private String nombre;

    protected Ruta() {
    }

    public Ruta(Long id, Long eventoId, String nombre) {
        this.id = id;
        this.eventoId = eventoId;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public String getNombre() { return nombre; }
}
