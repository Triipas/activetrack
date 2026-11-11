package com.activetrack.inscripciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de Inscripciones (productor de InscripcionReservada e InscripcionConfirmada).
 * Gestiona eventos, cupos, inscripciones, pagos y la autenticación de organizadores y jueces.
 */
@SpringBootApplication
@EnableScheduling
public class InscripcionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(InscripcionesApplication.class, args);
    }
}
