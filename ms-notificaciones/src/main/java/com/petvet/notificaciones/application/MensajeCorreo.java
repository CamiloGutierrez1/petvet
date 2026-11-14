package com.petvet.notificaciones.application;

/** Asunto y cuerpo en texto plano de un correo ya armado. */
public record MensajeCorreo(String asunto, String cuerpo) {
}
