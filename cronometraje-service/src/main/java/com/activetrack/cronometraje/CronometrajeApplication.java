package com.activetrack.cronometraje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Servicio de Cronometraje (productor de eventos).
 * Recibe las lecturas de las antenas RFID, las guarda y publica AtletaCruzoPuntoControl
 * sin esperar a que se calculen los tiempos.
 */
@SpringBootApplication
public class CronometrajeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CronometrajeApplication.class, args);
    }
}
