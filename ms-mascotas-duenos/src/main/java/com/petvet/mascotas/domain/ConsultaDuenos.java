package com.petvet.mascotas.domain;

import java.util.List;
import java.util.Optional;

/** Lecturas de dueños. Separado de {@link RegistroDuenos} para que cada caso de uso dependa solo de lo que usa. */
public interface ConsultaDuenos {
    Optional<Dueno> buscarPorId(long id);

    /** Busca por el correo ya normalizado: sin espacios y en minúsculas. */
    Optional<Dueno> buscarPorCorreo(String correo);

    /** Todos los dueños, ordenados por id. */
    List<Dueno> listar();
}
