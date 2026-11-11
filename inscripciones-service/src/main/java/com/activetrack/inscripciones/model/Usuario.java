package com.activetrack.inscripciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Personal con acceso restringido: organizadores y jueces. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private Long id;

    private String usuario;

    @Column(name = "clave_hash")
    private String claveHash;

    private String rol;
    private String nombre;

    protected Usuario() {
    }

    public Usuario(Long id, String usuario, String claveHash, String rol, String nombre) {
        this.id = id;
        this.usuario = usuario;
        this.claveHash = claveHash;
        this.rol = rol;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getClaveHash() { return claveHash; }
    public String getRol() { return rol; }
    public String getNombre() { return nombre; }
}
