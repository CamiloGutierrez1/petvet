package com.petvet.notificaciones.domain;

/**
 * El destino no es una dirección que el canal pueda usar, por ejemplo "laura,mendez@correo.com".
 * Falla igual en cada intento, así que el consumidor no lo reintenta: el mensaje va directo a la DLQ.
 */
public class DestinoInvalidoException extends RuntimeException {
    public DestinoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
