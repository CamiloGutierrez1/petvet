package com.petvet.mascotas.domain;

import java.util.List;
import java.util.Optional;

public interface MascotaRepository {

    // Registrar un nuevo mascota o guardar las actualizaciones
    Mascota guardar(Mascota mascota);

    // Buscar un mascota usando su ID
    Optional<Mascota> buscarPorId(Long id);

    // Buscar un mascota usando el ID del dueño
    List<Mascota> buscarPorDueno(Long duenoId);

    // Listar todos los mascotas registrados
    List<Mascota> listarTodas();

    // Actualizar la informacion del mascota
    Mascota actualizar(Mascota mascota);

    // Eliminar un mascota usando su ID
    void eliminar(Long id);
}
