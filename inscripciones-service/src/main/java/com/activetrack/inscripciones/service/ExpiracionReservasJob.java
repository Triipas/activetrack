package com.activetrack.inscripciones.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada 15 segundos libera las reservas vencidas y promueve a la lista de espera. */
@Component
public class ExpiracionReservasJob {

    private final ListaEsperaService listaEspera;

    public ExpiracionReservasJob(ListaEsperaService listaEspera) {
        this.listaEspera = listaEspera;
    }

    @Scheduled(fixedDelayString = "${inscripciones.revision-expiracion-ms}", initialDelay = 10_000)
    public void revisar() {
        listaEspera.expirarVencidas().forEach(listaEspera::promover);
    }
}
