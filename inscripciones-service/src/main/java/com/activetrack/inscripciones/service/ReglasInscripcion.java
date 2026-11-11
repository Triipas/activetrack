package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.model.Atleta;
import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.Tarifa;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

/** Reglas puras de inscripción (sin dependencias, fáciles de probar). */
public final class ReglasInscripcion {

    private ReglasInscripcion() {
    }

    /** Precio dinámico: la tarifa cuyo rango de fechas contiene el día de hoy (ej. Early Bird). */
    public static Optional<Tarifa> tarifaVigente(Collection<Tarifa> tarifas, LocalDate hoy) {
        return tarifas.stream().filter(t -> t.vigenteEn(hoy)).findFirst();
    }

    /** La edad se calcula a la fecha del evento, como en los reglamentos deportivos. */
    public static void validarCategoria(Categoria categoria, Atleta atleta, LocalDate fechaEvento) {
        int edad = atleta.edadEn(fechaEvento);
        if (!categoria.admite(edad, atleta.getGenero())) {
            throw new ReglaNegocioException(String.format(
                    "La categoría %s es para %s de %d a %d años; el atleta tendrá %d años el día del evento",
                    categoria.getNombre(), descripcionGenero(categoria.getGenero()),
                    categoria.getEdadMin(), categoria.getEdadMax(), edad));
        }
    }

    public static void validarTarifaNueva(Collection<Tarifa> existentes, Tarifa nueva) {
        existentes.stream().filter(t -> t.seCruzaCon(nueva)).findFirst().ifPresent(t -> {
            throw new ReglaNegocioException("Las fechas se cruzan con la tarifa " + t.getNombre());
        });
    }

    private static String descripcionGenero(String genero) {
        return switch (genero.toUpperCase()) {
            case "M" -> "varones";
            case "F" -> "damas";
            default -> "todos";
        };
    }
}
