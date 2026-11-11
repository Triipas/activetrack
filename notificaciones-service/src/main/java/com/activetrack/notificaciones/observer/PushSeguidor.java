package com.activetrack.notificaciones.observer;

import com.activetrack.notificaciones.service.PushGateway;

import java.time.Duration;

/**
 * Patrón Observer — rol <b>ConcreteObserver</b>.
 * Envía una notificación push al celular del espectador.
 */
public class PushSeguidor implements SeguidorAtleta {

    private final String tokenDispositivo;
    private final PushGateway gateway;

    public PushSeguidor(String tokenDispositivo, PushGateway gateway) {
        this.tokenDispositivo = tokenDispositivo;
        this.gateway = gateway;
    }

    @Override
    public void actualizar(CruceDTO cruce) {
        String titulo = cruce.nombre() + " (" + cruce.dorsal() + ")";
        String cuerpo = (cruce.esMeta() ? "¡Llegó a la meta! " : "Pasó por " + cruce.puntoControl() + " · ")
                + formatear(cruce.tiempoNetoMs())
                + (cruce.posicionGeneral() != null ? " · Puesto " + cruce.posicionGeneral() : "");
        gateway.enviar(tokenDispositivo, titulo, cuerpo);
    }

    @Override
    public String id() {
        return "push:" + tokenDispositivo;
    }

    static String formatear(long ms) {
        Duration d = Duration.ofMillis(ms);
        return String.format("%d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }
}
