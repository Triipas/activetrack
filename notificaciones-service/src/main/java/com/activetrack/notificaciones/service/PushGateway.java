package com.activetrack.notificaciones.service;

/**
 * Puerta de salida hacia el proveedor de notificaciones push (Firebase Cloud Messaging en producción).
 * PushSeguidor depende de esta abstracción, no de Firebase (Inversión de dependencias).
 */
public interface PushGateway {

    void enviar(String tokenDispositivo, String titulo, String cuerpo);
}
