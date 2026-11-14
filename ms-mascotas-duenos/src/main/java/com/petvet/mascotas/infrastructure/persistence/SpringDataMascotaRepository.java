package com.petvet.mascotas.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMascotaRepository extends JpaRepository<MascotaEntity, Long> {
    List<MascotaEntity> findByDuenoIdOrderByIdAsc(long duenoId);
}
