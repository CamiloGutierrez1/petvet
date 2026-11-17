package com.petvet.mascotas.application;

import com.petvet.mascotas.domain.Dueno;
import com.petvet.mascotas.domain.Mascota;

/** Mascota con su dueño: la base de la ficha plana que consumen Citas y el front. */
public record FichaMascota(Mascota mascota, Dueno dueno) {
}
