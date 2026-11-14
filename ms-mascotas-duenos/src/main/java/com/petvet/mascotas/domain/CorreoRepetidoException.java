package com.petvet.mascotas.domain;

/** Otro dueño ya usa el correo. La lanzan el registro y el adaptador cuando la base rechaza el duplicado. */
public class CorreoRepetidoException extends ConflictoException {
    public CorreoRepetidoException() {
        super("Ya existe un dueño registrado con ese correo.");
    }
}
