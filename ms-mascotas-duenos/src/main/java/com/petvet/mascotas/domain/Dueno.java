package com.petvet.mascotas.domain;

/** Dueño de mascotas. El correo se guarda sin espacios y en minúsculas; documento, telefono y direccion son opcionales. */
public record Dueno(Long id, String nombre, String documento, String email, String telefono, String direccion) {
}
