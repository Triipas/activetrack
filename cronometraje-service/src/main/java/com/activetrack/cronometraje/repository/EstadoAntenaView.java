package com.activetrack.cronometraje.repository;

import java.math.BigDecimal;

/** Proyección de la consulta de estado de antenas. */
public interface EstadoAntenaView {
    String getAntenaId();
    String getPuntoControl();
    BigDecimal getKm();
    Integer getOrden();
    Long getTotalLecturas();
    /** Epoch en milisegundos de la última señal recibida (null si nunca leyó). */
    Long getUltimaSenalMs();
}
