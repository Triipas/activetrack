package com.activetrack.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de Notificaciones (consumidor de ResultadoActualizado).
 * Avisa a los espectadores que siguen a un atleta usando el patrón Observer.
 */
@SpringBootApplication
public class NotificacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacionesApplication.class, args);
    }
}
