package com.activetrack.notificaciones.observer;

/**
 * Patrón Observer — rol <b>Observer</b>.
 * Cualquier canal que quiera enterarse de los cruces de un atleta implementa esta interfaz.
 * Un canal nuevo (SMS, correo...) es una clase nueva: no se modifica AtletaSeguido (Abierto/Cerrado).
 */
public interface SeguidorAtleta {

    void actualizar(CruceDTO cruce);

    /** Identificador del seguidor (sesión WebSocket o token del dispositivo). */
    String id();

    /** false cuando el canal ya no puede recibir (ej. el navegador se cerró). */
    default boolean activo() {
        return true;
    }
}
