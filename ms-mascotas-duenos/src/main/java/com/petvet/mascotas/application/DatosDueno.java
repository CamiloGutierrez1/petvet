package com.petvet.mascotas.application;

/** Datos de registro de un dueño. documento, telefono y direccion son opcionales. */
public record DatosDueno(String nombre, String documento, String email, String telefono, String direccion) {
}
