package com.petvet.mascotas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Mascota de un dueño, con el peso en kg y la foto opcional. */
public record Mascota(Long id, String nombre, Especie especie, String raza, LocalDate fechaNacimiento,
                      BigDecimal peso, String fotoUrl, Long duenoId) {

    /** Devuelve una copia con otra fotoUrl; la mascota original no cambia. */
    public Mascota conFoto(String fotoUrl) {
        return new Mascota(id, nombre, especie, raza, fechaNacimiento, peso, fotoUrl, duenoId);
    }
}
