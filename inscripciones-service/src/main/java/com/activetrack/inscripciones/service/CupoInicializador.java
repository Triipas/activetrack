package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.repository.CategoriaRepository;
import com.activetrack.inscripciones.repository.InscripcionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al arrancar, carga en Redis los cupos disponibles de cada categoría (si aún no existen). */
@Component
public class CupoInicializador {

    private static final Logger log = LoggerFactory.getLogger(CupoInicializador.class);

    private final CategoriaRepository categorias;
    private final InscripcionRepository inscripciones;
    private final CupoService cupos;

    public CupoInicializador(CategoriaRepository categorias, InscripcionRepository inscripciones, CupoService cupos) {
        this.categorias = categorias;
        this.inscripciones = inscripciones;
        this.cupos = cupos;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inicializar() {
        for (Categoria c : categorias.findAll()) {
            long ocupados = inscripciones.countByCategoriaIdAndEstadoIn(c.getId(), EstadoInscripcion.OCUPAN_CUPO);
            cupos.inicializar(c.getId(), Math.max(0, c.getCupoMaximo() - ocupados));
        }
        log.info("Cupos cargados en Redis");
    }
}
