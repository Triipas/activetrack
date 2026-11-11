package com.activetrack.inscripciones.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Redis y RabbitMQ no participan de la transacción de PostgreSQL.
 * Estas utilidades coordinan ambos mundos: compensar en Redis si la transacción se revierte
 * y publicar eventos solo cuando los datos ya quedaron guardados.
 */
public final class Transacciones {

    private Transacciones() {
    }

    /** Ejecuta la acción si la transacción actual se revierte (compensación). */
    public static void siSeRevierte(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int estado) {
                    if (estado == STATUS_ROLLED_BACK) {
                        accion.run();
                    }
                }
            });
        }
    }

    /** Ejecuta la acción cuando la transacción se confirma (o de inmediato si no hay transacción). */
    public static void alConfirmar(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
