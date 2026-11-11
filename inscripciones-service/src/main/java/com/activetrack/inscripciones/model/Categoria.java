package com.activetrack.inscripciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria")
public class Categoria {

    /** Género de una categoría abierta a todos. */
    public static final String MIXTA = "X";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id")
    private Long eventoId;

    private String nombre;

    @Column(name = "edad_min")
    private int edadMin;

    @Column(name = "edad_max")
    private int edadMax;

    private String genero;

    @Column(name = "cupo_maximo")
    private int cupoMaximo;

    protected Categoria() {
    }

    public Categoria(Long eventoId, String nombre, int edadMin, int edadMax, String genero, int cupoMaximo) {
        this.eventoId = eventoId;
        this.nombre = nombre;
        this.edadMin = edadMin;
        this.edadMax = edadMax;
        this.genero = genero;
        this.cupoMaximo = cupoMaximo;
    }

    /** ¿Un atleta de esta edad (a la fecha del evento) y género puede inscribirse aquí? */
    public boolean admite(int edad, String generoAtleta) {
        boolean generoValido = MIXTA.equals(genero) || genero.equalsIgnoreCase(generoAtleta);
        return generoValido && edad >= edadMin && edad <= edadMax;
    }

    public void cambiarCupo(int nuevoCupo) {
        this.cupoMaximo = nuevoCupo;
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public String getNombre() { return nombre; }
    public int getEdadMin() { return edadMin; }
    public int getEdadMax() { return edadMax; }
    public String getGenero() { return genero; }
    public int getCupoMaximo() { return cupoMaximo; }
}
