package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.model.Atleta;
import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.Tarifa;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasInscripcionTest {

    private final Tarifa earlyBird = new Tarifa(2L, "Early Bird", new BigDecimal("80"),
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
    private final Tarifa regular = new Tarifa(2L, "Regular", new BigDecimal("100"),
            LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 15));
    private final LocalDate fechaEvento = LocalDate.of(2027, 3, 21);

    @Test
    void eligeLaTarifaVigenteSegunLaFecha() {
        assertThat(ReglasInscripcion.tarifaVigente(List.of(earlyBird, regular), LocalDate.of(2026, 12, 31)))
                .contains(earlyBird);
        assertThat(ReglasInscripcion.tarifaVigente(List.of(earlyBird, regular), LocalDate.of(2027, 1, 1)))
                .contains(regular);
        assertThat(ReglasInscripcion.tarifaVigente(List.of(earlyBird, regular), LocalDate.of(2027, 3, 16)))
                .isEmpty();
    }

    @Test
    void rechazaTarifasQueSeCruzan() {
        Tarifa cruzada = new Tarifa(2L, "Promo", BigDecimal.TEN, LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15));
        assertThatThrownBy(() -> ReglasInscripcion.validarTarifaNueva(List.of(earlyBird, regular), cruzada))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void laEdadSeCalculaAlDiaDelEvento() {
        Categoria varones18a39 = new Categoria(2L, "Varones 18-39", 18, 39, "M", 100);
        // Cumple 40 el 20 de marzo de 2027: el día del evento ya tiene 40.
        Atleta cumple40 = new Atleta("Juan", "71000000", LocalDate.of(1987, 3, 20), "M", "j@x.com");
        // Cumple 18 el 21 de marzo de 2027: el día del evento ya tiene 18.
        Atleta cumple18 = new Atleta("Leo", "71000001", LocalDate.of(2009, 3, 21), "M", "l@x.com");

        assertThatThrownBy(() -> ReglasInscripcion.validarCategoria(varones18a39, cumple40, fechaEvento))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("40 años");
        assertThatCode(() -> ReglasInscripcion.validarCategoria(varones18a39, cumple18, fechaEvento))
                .doesNotThrowAnyException();
    }

    @Test
    void validaElGeneroDeLaCategoria() {
        Categoria damas = new Categoria(2L, "Damas 18-39", 18, 39, "F", 100);
        Categoria mixta = new Categoria(2L, "Open", 18, 99, Categoria.MIXTA, 100);
        Atleta varon = new Atleta("Ana", "71000002", LocalDate.of(1995, 1, 1), "M", "a@x.com");

        assertThatThrownBy(() -> ReglasInscripcion.validarCategoria(damas, varon, fechaEvento))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatCode(() -> ReglasInscripcion.validarCategoria(mixta, varon, fechaEvento))
                .doesNotThrowAnyException();
    }
}
