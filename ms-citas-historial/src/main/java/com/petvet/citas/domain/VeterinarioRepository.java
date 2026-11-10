package com.petvet.citas.domain;

import java.util.List;
import java.util.Optional;

public interface VeterinarioRepository {
    List<Veterinario> listar();

    Optional<Veterinario> buscarPorId(long id);
}
