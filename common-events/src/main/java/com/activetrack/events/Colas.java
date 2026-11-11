package com.activetrack.events;

/**
 * Nombres del exchange, routing keys y colas de RabbitMQ.
 * Centralizarlos evita que productores y consumidores usen nombres distintos.
 */
public final class Colas {

    /** Exchange de tipo topic por donde viajan todos los eventos de negocio. */
    public static final String EXCHANGE = "activetrack.eventos";
    /** Exchange donde terminan los mensajes que fallaron tras los reintentos. */
    public static final String DLX = "activetrack.dlx";

    public static final String RK_CRUCE = "cronometraje.cruce";
    public static final String RK_RESULTADO = "resultados.actualizado";
    public static final String RK_INSCRIPCION_RESERVADA = "inscripciones.reservada";
    public static final String RK_INSCRIPCION_CONFIRMADA = "inscripciones.confirmada";
    /** Patrón que escucha todos los eventos de inscripción. */
    public static final String RK_INSCRIPCIONES = "inscripciones.#";

    public static final String COLA_RESULTADOS_CRUCES = "resultados.cruces";
    public static final String COLA_NOTIFICACIONES_RESULTADOS = "notificaciones.resultados";
    public static final String COLA_NOTIFICACIONES_INSCRIPCIONES = "notificaciones.inscripciones";

    private Colas() {
    }
}
