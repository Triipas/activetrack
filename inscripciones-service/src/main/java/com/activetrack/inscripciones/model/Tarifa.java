package com.activetrack.inscripciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Precio de inscripción vigente entre dos fechas (ej. Early Bird, Regular). */
@Entity
@Table(name = "tarifa")
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id")
    private Long eventoId;

    private String nombre;
    private BigDecimal precio;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    protected Tarifa() {
    }

    public Tarifa(Long eventoId, String nombre, BigDecimal precio, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        if (vigenteHasta.isBefore(vigenteDesde)) {
            throw new IllegalArgumentException("La tarifa " + nombre + " termina antes de empezar");
        }
        this.eventoId = eventoId;
        this.nombre = nombre;
        this.precio = precio;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
    }

    public boolean vigenteEn(LocalDate fecha) {
        return !fecha.isBefore(vigenteDesde) && !fecha.isAfter(vigenteHasta);
    }

    public boolean seCruzaCon(Tarifa otra) {
        return !vigenteHasta.isBefore(otra.vigenteDesde) && !otra.vigenteHasta.isBefore(vigenteDesde);
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
}
