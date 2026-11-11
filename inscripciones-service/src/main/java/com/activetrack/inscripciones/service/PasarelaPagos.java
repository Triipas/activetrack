package com.activetrack.inscripciones.service;

import java.math.BigDecimal;

/**
 * Puerta de salida hacia la pasarela de pagos externa (Culqi, Niubiz, Stripe...).
 * PagoService depende de esta abstracción y no de un proveedor concreto.
 */
public interface PasarelaPagos {

    /** Registra el cobro y devuelve la URL donde el atleta completa el pago. */
    String crearCobro(String referencia, BigDecimal monto, String descripcion);

    void reembolsar(String referencia, BigDecimal monto);
}
