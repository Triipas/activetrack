package com.activetrack.resultados.model;

public enum EstadoResultado {
    /** Corriendo, con todos los puntos de control registrados. */
    EN_CARRERA,
    /** Se detectó un punto de control omitido: espera revisión del juez. */
    OBSERVADO,
    /** Cruzó la meta sin observaciones pendientes. */
    FINALIZADO,
    /** Descalificado por el juez. */
    DSQ
}
