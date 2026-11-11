package com.activetrack.inscripciones.service;

import com.activetrack.events.InscripcionConfirmada;
import com.activetrack.events.InscripcionReservada;
import com.activetrack.inscripciones.model.Atleta;
import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.Evento;
import com.activetrack.inscripciones.model.Inscripcion;
import com.activetrack.inscripciones.model.Tarifa;

import java.math.BigDecimal;
import java.time.Instant;

/** Una inscripción junto con su atleta, categoría, evento y tarifa. Arma los eventos y el detalle. */
public record FichaInscripcion(Inscripcion inscripcion, Atleta atleta, Categoria categoria, Evento evento, Tarifa tarifa) {

    public record Detalle(Long id, Long eventoId, String evento, Instant fechaEvento, String categoria,
                          String atleta, String documento, String estado, Integer dorsal, String chipId,
                          String codigoQr, BigDecimal monto, String tarifa, Instant expiraEn, Long posicionEspera) {
    }

    public InscripcionReservada eventoReservada() {
        return new InscripcionReservada(inscripcion.getId(), evento.getId(), evento.getNombre(), categoria.getNombre(),
                atleta.getNombres(), atleta.getEmail(), tarifa.getPrecio(), inscripcion.getExpiraEn());
    }

    public InscripcionConfirmada eventoConfirmada() {
        return new InscripcionConfirmada(inscripcion.getId(), evento.getId(), evento.getNombre(), categoria.getNombre(),
                atleta.getNombres(), atleta.getEmail(), inscripcion.getDorsal(), inscripcion.getChipId(),
                inscripcion.getCodigoQr());
    }

    public Detalle detalle(Long posicionEspera) {
        return new Detalle(inscripcion.getId(), evento.getId(), evento.getNombre(), evento.getFecha(),
                categoria.getNombre(), atleta.getNombres(), atleta.getDocumento(), inscripcion.getEstado().name(),
                inscripcion.getDorsal(), inscripcion.getChipId(), inscripcion.getCodigoQr(), tarifa.getPrecio(),
                tarifa.getNombre(), inscripcion.getExpiraEn(), posicionEspera);
    }
}
