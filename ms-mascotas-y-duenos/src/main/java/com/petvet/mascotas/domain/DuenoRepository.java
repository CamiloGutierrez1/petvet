package com.petvet.mascotas.domain;

import java.util.List;
import java.util.Optional;

public interface DuenoRepository {

    // Registrar un nuevo dueño o guardar las actualizaciones
    Dueno guardar(Dueno dueno);

    // Buscar un dueño usando su ID
    Optional<Dueno> buscarPorId(Long duenoId);

    // Buscar un dueño usando su correo electronico
    Optional<Dueno> buscarPorCorreo(String correoDueno);

    // Listar todos los dueños registrados
    List<Dueno> listar();

    // Actualizar la informacion del dueño
    Dueno actualizar(Dueno dueno);

    // Eliminar dueño usando su ID
    void eliminar(Long duenoId);
}
