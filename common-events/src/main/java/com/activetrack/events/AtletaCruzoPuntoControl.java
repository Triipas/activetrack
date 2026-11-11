package com.activetrack.events;

import java.time.Instant;

/**
 * Evento publicado por el Servicio de Cronometraje cuando el chip de un atleta
 * es detectado por una antena RFID.
 *
 * @param lecturaId      id de la lectura guardada
 * @param eventoId       evento deportivo
 * @param inscripcionId  inscripción (chip) del atleta
 * @param puntoControlId punto de control cruzado
 * @param ordenPunto     orden del punto en la ruta (0 = salida)
 * @param esMeta         true si el punto es la meta
 * @param marcaTiempo    hora en que el atleta pisó la alfombra
 * @param recibidoEn     hora en que el sistema recibió la lectura (para medir latencia)
 */
public record AtletaCruzoPuntoControl(
        Long lecturaId,
        Long eventoId,
        Long inscripcionId,
        Long puntoControlId,
        int ordenPunto,
        boolean esMeta,
        Instant marcaTiempo,
        Instant recibidoEn) {
}
