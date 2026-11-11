package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.model.Atleta;
import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.model.Inscripcion;
import com.activetrack.inscripciones.repository.AtletaRepository;
import com.activetrack.inscripciones.repository.CategoriaRepository;
import com.activetrack.inscripciones.repository.EventoRepository;
import com.activetrack.inscripciones.repository.InscripcionRepository;
import com.activetrack.inscripciones.repository.TarifaRepository;
import org.springframework.stereotype.Service;

/** Carga los datos relacionados de una inscripción. */
@Service
public class FichaService {

    private final InscripcionRepository inscripciones;
    private final AtletaRepository atletas;
    private final CategoriaRepository categorias;
    private final EventoRepository eventos;
    private final TarifaRepository tarifas;

    public FichaService(InscripcionRepository inscripciones, AtletaRepository atletas, CategoriaRepository categorias,
                        EventoRepository eventos, TarifaRepository tarifas) {
        this.inscripciones = inscripciones;
        this.atletas = atletas;
        this.categorias = categorias;
        this.eventos = eventos;
        this.tarifas = tarifas;
    }

    public FichaInscripcion de(Inscripcion inscripcion) {
        Atleta atleta = atletas.findById(inscripcion.getAtletaId()).orElseThrow();
        Categoria categoria = categorias.findById(inscripcion.getCategoriaId()).orElseThrow();
        return new FichaInscripcion(inscripcion, atleta, categoria,
                eventos.findById(categoria.getEventoId()).orElseThrow(),
                tarifas.findById(inscripcion.getTarifaId()).orElseThrow());
    }

    /** Busca la inscripción comprobando que pertenece al atleta del documento indicado. */
    public FichaInscripcion delAtleta(Long inscripcionId, String documento) {
        Inscripcion inscripcion = inscripciones.findById(inscripcionId)
                .orElseThrow(() -> new NoEncontradoException("Inscripción no encontrada"));
        FichaInscripcion ficha = de(inscripcion);
        if (!ficha.atleta().getDocumento().equals(documento)) {
            throw new NoEncontradoException("Inscripción no encontrada");
        }
        return ficha;
    }

    /** Posición en la lista de espera (1 = el siguiente en recibir cupo). */
    public Long posicionEspera(Inscripcion inscripcion) {
        if (inscripcion.getEstado() != EstadoInscripcion.EN_ESPERA) {
            return null;
        }
        return inscripciones.countByCategoriaIdAndEstadoAndCreadaEnLessThan(
                inscripcion.getCategoriaId(), EstadoInscripcion.EN_ESPERA, inscripcion.getCreadaEn()) + 1;
    }
}
