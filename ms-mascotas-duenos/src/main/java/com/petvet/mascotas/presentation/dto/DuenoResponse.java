package com.petvet.mascotas.presentation.dto;

import com.petvet.mascotas.domain.Dueno;

/** Dueño en las respuestas. documento, telefono y direccion pueden ser null. */
public record DuenoResponse(Long id, String nombre, String documento, String email, String telefono,
                            String direccion) {

    public static DuenoResponse desde(Dueno dueno) {
        return new DuenoResponse(dueno.id(), dueno.nombre(), dueno.documento(), dueno.email(), dueno.telefono(),
                dueno.direccion());
    }
}
