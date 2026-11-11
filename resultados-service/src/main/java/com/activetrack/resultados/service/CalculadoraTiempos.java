package com.activetrack.resultados.service;

import java.time.Duration;
import java.time.Instant;

/** Reglas puras de cálculo de tiempos (sin dependencias, fácil de probar). */
public final class CalculadoraTiempos {

    private CalculadoraTiempos() {
    }

    /**
     * @param largadaOficial    hora oficial de largada del evento
     * @param salidaChip        hora en que el chip pisó la salida (null si la antena no lo leyó)
     * @param cruce             hora del último punto de control cruzado
     * @param penalizacionSeg   segundos de penalización acumulados
     */
    public static Tiempos calcular(Instant largadaOficial, Instant salidaChip, Instant cruce, long penalizacionSeg) {
        if (cruce.isBefore(largadaOficial)) {
            throw new IllegalArgumentException("El cruce no puede ser anterior a la largada oficial");
        }
        long penalizacionMs = penalizacionSeg * 1000;
        long bruto = Duration.between(largadaOficial, cruce).toMillis() + penalizacionMs;
        // Sin lectura de salida, el tiempo neto se toma igual al bruto (criterio conservador).
        Instant inicioNeto = salidaChip != null && !salidaChip.isAfter(cruce) ? salidaChip : largadaOficial;
        long neto = Duration.between(inicioNeto, cruce).toMillis() + penalizacionMs;
        return new Tiempos(bruto, neto);
    }
}
