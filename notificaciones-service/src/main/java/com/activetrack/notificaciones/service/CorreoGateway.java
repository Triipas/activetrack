package com.activetrack.notificaciones.service;

/** Puerta de salida hacia el proveedor de correo (Amazon SES, SendGrid...). */
public interface CorreoGateway {

    void enviar(String destinatario, String asunto, String cuerpo);
}
