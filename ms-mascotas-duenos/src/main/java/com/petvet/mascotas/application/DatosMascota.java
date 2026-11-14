package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.Especie;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Cuerpo del alta y de la edición de una mascota: los únicos campos que el dueño puede cambiar. */
public record DatosMascota(String nombre, Especie especie, String raza, LocalDate fechaNacimiento,
                           BigDecimal peso) {
}
