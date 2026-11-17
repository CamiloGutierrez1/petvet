package com.petvet.citas.infrastructure.persistence;

import com.petvet.citas.domain.HistorialRepository;
import com.petvet.citas.domain.RegistroHistorial;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaHistorialRepository implements HistorialRepository {
    private final SpringDataHistorialRepository repository;

    public JpaHistorialRepository(SpringDataHistorialRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public RegistroHistorial guardar(RegistroHistorial registro) {
        return repository.saveAndFlush(RegistroHistorialEntity.fromDomain(registro)).toDomain();
    }

    @Override
    public List<RegistroHistorial> porMascota(long mascotaId) {
        return repository.findByMascotaIdOrderByFechaDescIdDesc(mascotaId).stream()
                .map(RegistroHistorialEntity::toDomain)
                .toList();
    }

}
