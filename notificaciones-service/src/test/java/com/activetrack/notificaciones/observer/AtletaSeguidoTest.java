package com.activetrack.notificaciones.observer;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AtletaSeguidoTest {

    /** Observer de prueba que guarda lo que recibe. */
    static class SeguidorDePrueba implements SeguidorAtleta {
        final String id;
        final List<CruceDTO> recibidos = new ArrayList<>();
        boolean activo = true;
        boolean falla = false;

        SeguidorDePrueba(String id) {
            this.id = id;
        }

        @Override
        public void actualizar(CruceDTO cruce) {
            if (falla) {
                throw new IllegalStateException("canal caído");
            }
            recibidos.add(cruce);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean activo() {
            return activo;
        }
    }

    private final CruceDTO cruce = new CruceDTO(1L, 7L, 7L, "Jorge Vargas", 107, "Varones 40+",
            "Control 10K", 10, false, 2_400_000, 3L, 1L, "EN_CARRERA", Instant.now());

    @Test
    void notificaATodosLosSeguidoresSuscritos() {
        AtletaSeguido atleta = new AtletaSeguido(7L);
        SeguidorDePrueba a = new SeguidorDePrueba("a");
        SeguidorDePrueba b = new SeguidorDePrueba("b");
        atleta.suscribir(a);
        atleta.suscribir(b);

        atleta.registrarCruce(cruce);

        assertThat(a.recibidos).containsExactly(cruce);
        assertThat(b.recibidos).containsExactly(cruce);
    }

    @Test
    void quienSeDesuscribeYaNoRecibe() {
        AtletaSeguido atleta = new AtletaSeguido(7L);
        SeguidorDePrueba a = new SeguidorDePrueba("a");
        atleta.suscribir(a);
        atleta.desuscribir("a");

        atleta.registrarCruce(cruce);

        assertThat(a.recibidos).isEmpty();
        assertThat(atleta.cantidadSeguidores()).isZero();
    }

    @Test
    void unCanalQueFallaNoAfectaALosDemas() {
        AtletaSeguido atleta = new AtletaSeguido(7L);
        SeguidorDePrueba roto = new SeguidorDePrueba("roto");
        roto.falla = true;
        SeguidorDePrueba sano = new SeguidorDePrueba("sano");
        atleta.suscribir(roto);
        atleta.suscribir(sano);

        atleta.registrarCruce(cruce);

        assertThat(sano.recibidos).hasSize(1);
    }

    @Test
    void retiraSeguidoresInactivosYNoDuplicaSuscripciones() {
        AtletaSeguido atleta = new AtletaSeguido(7L);
        SeguidorDePrueba a = new SeguidorDePrueba("a");
        atleta.suscribir(a);
        atleta.suscribir(a);
        assertThat(atleta.cantidadSeguidores()).isEqualTo(1);

        a.activo = false;
        atleta.registrarCruce(cruce);

        assertThat(atleta.cantidadSeguidores()).isZero();
        assertThat(a.recibidos).isEmpty();
    }
}
