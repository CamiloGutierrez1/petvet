package com.petvet.notificaciones.domain;

import java.time.Instant;

/**
 * Correo enviado por un mensaje de RabbitMQ. {@code messageId} es la clave de deduplicación: un
 * mismo mensaje nunca genera dos notificaciones. {@code id} es nulo hasta guardarla.
 */
public record Notificacion(
        Long id,
        String messageId,
        TipoNotificacion tipo,
        String destino,
        String asunto,
        String cuerpo,
        Instant creadaEn,
        Instant enviadaEn) {

    public Notificacion {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("La notificación necesita el messageId del mensaje");
        }
        if (tipo == null || destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("La notificación necesita tipo y destino");
        }
        if (asunto == null || asunto.isBlank() || cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException("La notificación necesita asunto y cuerpo");
        }
        if (creadaEn == null || enviadaEn == null) {
            throw new IllegalArgumentException("La notificación necesita sus fechas de creación y envío");
        }
    }
}
