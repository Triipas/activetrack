package com.activetrack.cronometraje.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "lectura_rfid")
public class LecturaRfid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inscripcion_id")
    private Long inscripcionId;

    @Column(name = "punto_control_id")
    private Long puntoControlId;

    @Column(name = "antena_id")
    private String antenaId;

    @Column(name = "marca_tiempo")
    private Instant marcaTiempo;

    @Column(name = "recibido_en")
    private Instant recibidoEn;

    protected LecturaRfid() {
    }

    public LecturaRfid(Long inscripcionId, Long puntoControlId, String antenaId, Instant marcaTiempo, Instant recibidoEn) {
        this.inscripcionId = inscripcionId;
        this.puntoControlId = puntoControlId;
        this.antenaId = antenaId;
        this.marcaTiempo = marcaTiempo;
        this.recibidoEn = recibidoEn;
    }

    public Long getId() { return id; }
    public Long getInscripcionId() { return inscripcionId; }
    public Long getPuntoControlId() { return puntoControlId; }
    public String getAntenaId() { return antenaId; }
    public Instant getMarcaTiempo() { return marcaTiempo; }
    public Instant getRecibidoEn() { return recibidoEn; }
}
