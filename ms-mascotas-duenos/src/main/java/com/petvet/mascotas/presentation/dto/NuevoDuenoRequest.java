package com.petvet.mascotas.presentation.dto;

import com.petvet.mascotas.application.DatosDueno;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/duenos. Los tamaños son los de las columnas, para que un texto largo responda 400
 * y no un error de la base. El formato del correo lo valida el caso de uso después de quitar espacios.
 * Los opcionales vacíos se guardan como null, por eso el teléfono admite un texto en blanco.
 */
public record NuevoDuenoRequest(
        @NotBlank(message = "El nombre del dueño es obligatorio.")
        @Size(max = 100, message = "El nombre del dueño admite máximo 100 caracteres.")
        String nombre,
        @Size(max = 30, message = "El documento admite máximo 30 caracteres.")
        String documento,
        @NotBlank(message = "El correo del dueño es obligatorio.")
        @Size(max = 254, message = "El correo del dueño admite máximo 254 caracteres.")
        String email,
        @Pattern(regexp = "\\s*(\\d{7,10})?\\s*",
                message = "El teléfono debe tener solo números, entre 7 y 10 dígitos.")
        String telefono,
        @Size(max = 200, message = "La dirección admite máximo 200 caracteres.")
        String direccion) {

    public DatosDueno aDatos() {
        return new DatosDueno(nombre, documento, email, telefono, direccion);
    }
}
