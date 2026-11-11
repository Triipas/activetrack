package com.activetrack.resultados.model;

public enum TipoPenalizacion {
    /** Suma segundos al tiempo del atleta. */
    TIEMPO,
    /** Descalificación. */
    DSQ,
    /** El juez revisó la observación y no aplica sanción. */
    SIN_SANCION
}
