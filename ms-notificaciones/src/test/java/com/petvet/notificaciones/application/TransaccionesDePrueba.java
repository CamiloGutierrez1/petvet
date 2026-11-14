package com.petvet.notificaciones.application;

import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

/** Ejecuta el bloque sin base de datos y registra cuándo y cuántas veces se abrió una transacción. */
final class TransaccionesDePrueba implements TransactionOperations {
    private boolean activa;
    private int abiertas;

    @Override
    public <T> T execute(TransactionCallback<T> accion) {
        abiertas++;
        activa = true;
        try {
            return accion.doInTransaction(new SimpleTransactionStatus());
        } finally {
            activa = false;
        }
    }

    boolean activa() {
        return activa;
    }

    int abiertas() {
        return abiertas;
    }
}
