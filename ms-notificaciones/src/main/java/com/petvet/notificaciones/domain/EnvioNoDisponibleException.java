package com.petvet.notificaciones.domain;

/** El canal de envío no aceptó el mensaje. El consumidor lo reintenta y, si sigue fallando, va a la DLQ. */
public class EnvioNoDisponibleException extends RuntimeException {
    public EnvioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
