package com.petvet.mascotas.domain;

/** Escritura de dueños. Ninguna ruta de la API edita ni elimina dueños, por eso no hay más métodos. */
public interface RegistroDuenos {
    /** Guarda un dueño nuevo. Si otro dueño ya tiene el correo, lanza {@link CorreoRepetidoException}. */
    Dueno guardar(Dueno dueno);
}
