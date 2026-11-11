package com.activetrack.resultados.model;

import com.activetrack.resultados.service.Tiempos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Resultado vigente de un atleta en el evento. Las reglas de cambio de estado viven aquí. */
@Entity
@Table(name = "resultado")
public class Resultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inscripcion_id")
    private Long inscripcionId;

    @Column(name = "ultimo_punto_id")
    private Long ultimoPuntoId;

    @Column(name = "ultimo_punto_orden")
    private Integer ultimoPuntoOrden;

    @Column(name = "ultima_marca")
    private Instant ultimaMarca;

    @Column(name = "tiempo_bruto_ms")
    private Long tiempoBrutoMs;

    @Column(name = "tiempo_neto_ms")
    private Long tiempoNetoMs;

    @Column(name = "posicion_general")
    private Integer posicionGeneral;

    @Column(name = "posicion_categoria")
    private Integer posicionCategoria;

    @Enumerated(EnumType.STRING)
    private EstadoResultado estado;

    protected Resultado() {
    }

    public Resultado(Long inscripcionId) {
        this.inscripcionId = inscripcionId;
        this.estado = EstadoResultado.EN_CARRERA;
    }

    /** Un cruce solo cuenta si es de un punto posterior al último registrado. */
    public boolean esCruceNuevo(int ordenPunto) {
        return estado != EstadoResultado.DSQ && (ultimoPuntoOrden == null || ordenPunto > ultimoPuntoOrden);
    }

    public void registrarCruce(Long puntoId, int ordenPunto, Instant marca, boolean esMeta, boolean hayPuntosFaltantes) {
        this.ultimoPuntoId = puntoId;
        this.ultimoPuntoOrden = ordenPunto;
        this.ultimaMarca = marca;
        if (hayPuntosFaltantes) {
            this.estado = EstadoResultado.OBSERVADO;
        } else if (estado != EstadoResultado.OBSERVADO) {
            this.estado = esMeta ? EstadoResultado.FINALIZADO : EstadoResultado.EN_CARRERA;
        }
    }

    /** El juez cerró la observación (con o sin penalidad de tiempo). */
    public void cerrarObservacion(boolean enMeta) {
        this.estado = enMeta ? EstadoResultado.FINALIZADO : EstadoResultado.EN_CARRERA;
    }

    public void descalificar() {
        this.estado = EstadoResultado.DSQ;
    }

    public void actualizarTiempos(Tiempos tiempos) {
        this.tiempoBrutoMs = tiempos.brutoMs();
        this.tiempoNetoMs = tiempos.netoMs();
    }

    public void actualizarPosiciones(Long general, Long categoria) {
        this.posicionGeneral = general == null ? null : general.intValue();
        this.posicionCategoria = categoria == null ? null : categoria.intValue();
    }

    public Long getId() { return id; }
    public Long getInscripcionId() { return inscripcionId; }
    public Long getUltimoPuntoId() { return ultimoPuntoId; }
    public Integer getUltimoPuntoOrden() { return ultimoPuntoOrden; }
    public Instant getUltimaMarca() { return ultimaMarca; }
    public Long getTiempoBrutoMs() { return tiempoBrutoMs; }
    public Long getTiempoNetoMs() { return tiempoNetoMs; }
    public Integer getPosicionGeneral() { return posicionGeneral; }
    public Integer getPosicionCategoria() { return posicionCategoria; }
    public EstadoResultado getEstado() { return estado; }
}
