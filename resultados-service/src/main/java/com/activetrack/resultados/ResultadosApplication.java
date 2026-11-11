package com.activetrack.resultados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de Resultados (consumidor de AtletaCruzoPuntoControl y productor de ResultadoActualizado).
 * Calcula tiempos, mantiene el leaderboard en Redis y gestiona disputas y penalizaciones.
 */
@SpringBootApplication
public class ResultadosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResultadosApplication.class, args);
    }
}
