package com.activetrack.notificaciones.observer;

import com.activetrack.notificaciones.service.GestorSeguimientos;
import com.activetrack.notificaciones.service.PushGateway;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PushSeguidorTest {

    private final CruceDTO llegada = new CruceDTO(1L, 4L, 4L, "Ana Condori", 104, "Damas 18-39",
            "Meta", 42.195, true, 9_015_000, 5L, 1L, "FINALIZADO", Instant.now());

    @Test
    void elGestorEntregaElCruceAlSeguidorPush() {
        PushGateway gateway = mock(PushGateway.class);
        GestorSeguimientos gestor = new GestorSeguimientos();
        gestor.seguir(4L, new PushSeguidor("token-abc", gateway));

        gestor.registrarCruce(llegada);

        verify(gateway).enviar(eq("token-abc"), contains("Ana Condori"), contains("2:30:15"));
    }

    @Test
    void noNotificaASeguidoresDeOtroAtleta() {
        PushGateway gateway = mock(PushGateway.class);
        GestorSeguimientos gestor = new GestorSeguimientos();
        gestor.seguir(99L, new PushSeguidor("token-xyz", gateway));

        gestor.registrarCruce(llegada);

        verifyNoInteractions(gateway);
    }
}
