package com.petvet.mascotas.domain;

/** Escritura de los datos de una mascota. La foto tiene su propio puerto, {@link AlmacenFotosMascota}. */
public interface RegistroMascotas {
    /** Registra una mascota nueva, sin foto. */
    Mascota guardar(Mascota mascota);

    /** Cambia nombre, especie, raza, fecha de nacimiento y peso. Conserva la foto y el dueño. */
    Mascota actualizar(Mascota mascota);
}
