package com.activetrack.inscripciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Pasarela de pruebas para la PoC: el checkout es una página del frontend (pasarela.html) que,
 * al aprobar o rechazar, llama al webhook igual que lo haría la pasarela real.
 */
@Component
public class PasarelaSandbox implements PasarelaPagos {

    private static final Logger log = LoggerFactory.getLogger(PasarelaSandbox.class);

    @Override
    public String crearCobro(String referencia, BigDecimal monto, String descripcion) {
        log.info("SANDBOX cobro {} por S/ {} ({})", referencia, monto, descripcion);
        return "/pasarela.html?ref=" + referencia;
    }

    @Override
    public void reembolsar(String referencia, BigDecimal monto) {
        log.info("SANDBOX reembolso {} por S/ {}", referencia, monto);
    }
}
