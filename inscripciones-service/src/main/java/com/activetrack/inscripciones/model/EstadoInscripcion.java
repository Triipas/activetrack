package com.activetrack.inscripciones.model;

import java.util.List;

public enum EstadoInscripcion {
    /** Tiene cupo reservado y un plazo para pagar. */
    PENDIENTE_PAGO,
    /** Pagó: tiene dorsal, chip y kit digital. */
    CONFIRMADA,
    /** No pagó a tiempo: el cupo se liberó. */
    EXPIRADA,
    /** No había cupo: espera que se libere uno. */
    EN_ESPERA,
    /** Reembolsada por el organizador. */
    ANULADA;

    /** Estados que ocupan un cupo. */
    public static final List<EstadoInscripcion> OCUPAN_CUPO = List.of(PENDIENTE_PAGO, CONFIRMADA);
    /** Estados que impiden volver a inscribirse en el mismo evento. */
    public static final List<EstadoInscripcion> ACTIVAS = List.of(PENDIENTE_PAGO, CONFIRMADA, EN_ESPERA);
}
