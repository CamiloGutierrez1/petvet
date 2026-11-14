package com.petvet.notificaciones.domain;

/**
 * Puerto de salida del patrón Adapter: los casos de uso envían sin saber si el canal es SMTP u otro.
 */
public interface EnviadorNotificaciones {

    /**
     * Entrega el mensaje al destino.
     *
     * @throws EnvioNoDisponibleException si el canal no acepta el mensaje
     * @throws DestinoInvalidoException si el destino no es una dirección que el canal pueda usar
     */
    void enviar(String destino, String asunto, String cuerpo);
}
