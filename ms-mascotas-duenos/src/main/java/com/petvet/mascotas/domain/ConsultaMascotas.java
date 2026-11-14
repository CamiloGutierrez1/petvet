package com.petvet.mascotas.domain;

import java.util.List;
import java.util.Optional;

/** Lecturas de mascotas, separadas de {@link RegistroMascotas} y de {@link AlmacenFotosMascota}. */
public interface ConsultaMascotas {
    Optional<Mascota> buscarPorId(long id);

    /** Mascotas del dueño, ordenadas por id. */
    List<Mascota> buscarPorDueno(long duenoId);
}
