package com.petvet.mascotas.presentation.dto;

import com.petvet.mascotas.application.DatosMascota;
import com.petvet.mascotas.domain.Especie;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cuerpo de POST /api/duenos/{id}/mascotas y de PUT /api/mascotas/{id}. El peso cabe en NUMERIC(6,2)
 * sin redondear: así 0.004 no se guarda como 0 ni 0.035 como 0.04.
 */
public record DatosMascotaRequest(
        @NotBlank(message = "El nombre de la mascota es obligatorio.")
        @Size(max = 60, message = "El nombre de la mascota admite máximo 60 caracteres.")
        String nombre,
        @NotNull(message = "La especie es obligatoria: PERRO, GATO, AVE u OTRO.")
        Especie especie,
        @NotBlank(message = "La raza es obligatoria.")
        @Size(max = 60, message = "La raza admite máximo 60 caracteres.")
        String raza,
        @NotNull(message = "La fecha de nacimiento es obligatoria.")
        LocalDate fechaNacimiento,
        @NotNull(message = "El peso es obligatorio.")
        @DecimalMin(value = "0.01", message = "El peso debe ser mayor que 0.")
        @Digits(integer = 4, fraction = 2,
                message = "El peso admite hasta 4 enteros y 2 decimales, por ejemplo 31.5 o 0.04.")
        BigDecimal peso) {

    public DatosMascota aDatos() {
        return new DatosMascota(nombre, especie, raza, fechaNacimiento, peso);
    }
}
