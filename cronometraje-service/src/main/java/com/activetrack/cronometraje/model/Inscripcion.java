package com.activetrack.cronometraje.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Vista mínima de la inscripción: solo lo que el cronometraje necesita (chip y estado). */
@Entity
@Table(name = "inscripcion")
public class Inscripcion {

    public static final String CONFIRMADA = "CONFIRMADA";

    @Id
    private Long id;

    @Column(name = "chip_id")
    private String chipId;

    private Integer dorsal;
    private String estado;

    protected Inscripcion() {
    }

    public Inscripcion(Long id, String chipId, Integer dorsal, String estado) {
        this.id = id;
        this.chipId = chipId;
        this.dorsal = dorsal;
        this.estado = estado;
    }

    public boolean estaConfirmada() {
        return CONFIRMADA.equals(estado);
    }

    public Long getId() { return id; }
    public String getChipId() { return chipId; }
    public Integer getDorsal() { return dorsal; }
    public String getEstado() { return estado; }
}
