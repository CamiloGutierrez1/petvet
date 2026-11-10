package com.petvet.citas.domain;

import java.time.LocalDate;
import java.util.Objects;

public record Mascota(Long id, String nombre, Especie especie, LocalDate fechaNacimiento,
                      String duenoNombre, String duenoEmail) {

    public Mascota {
        Objects.requireNonNull(especie, "La especie es obligatoria");
        Objects.requireNonNull(fechaNacimiento, "La fecha de nacimiento es obligatoria");
    }
}
