package com.activetrack.inscripciones.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

@Entity
@Table(name = "evento")
public class Evento {

    public static final String INSCRIPCIONES_ABIERTAS = "INSCRIPCIONES_ABIERTAS";
    public static final Set<String> ESTADOS = Set.of(INSCRIPCIONES_ABIERTAS, "INSCRIPCIONES_CERRADAS", "EN_CURSO", "FINALIZADO");
    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String tipo;
    private Instant fecha;
    private String estado;

    protected Evento() {
    }

    public Evento(String nombre, String tipo, Instant fecha) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.fecha = fecha;
        this.estado = INSCRIPCIONES_ABIERTAS;
    }

    public boolean inscripcionesAbiertas() {
        return INSCRIPCIONES_ABIERTAS.equals(estado);
    }

    public LocalDate fechaLocal() {
        return fecha.atZone(ZONA).toLocalDate();
    }

    public void cambiarEstado(String nuevo) {
        if (!ESTADOS.contains(nuevo)) {
            throw new IllegalArgumentException("Estado no válido: " + nuevo);
        }
        this.estado = nuevo;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public Instant getFecha() { return fecha; }
    public String getEstado() { return estado; }
}
