package com.petvet.citas.infrastructure.persistence;

import com.petvet.citas.domain.Veterinario;
import com.petvet.citas.domain.VeterinarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaVeterinarioRepository implements VeterinarioRepository {
    private final SpringDataVeterinarioRepository repository;

    public JpaVeterinarioRepository(SpringDataVeterinarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Veterinario> listar() {
        return repository.findAllByOrderByIdAsc().stream().map(VeterinarioEntity::toDomain).toList();
    }

    @Override
    public Optional<Veterinario> buscarPorId(long id) {
        return repository.findById(id).map(VeterinarioEntity::toDomain);
    }
}
