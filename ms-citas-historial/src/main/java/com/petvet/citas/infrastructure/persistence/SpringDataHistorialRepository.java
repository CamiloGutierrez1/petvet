package com.petvet.citas.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataHistorialRepository extends JpaRepository<RegistroHistorialEntity, Long> {
    List<RegistroHistorialEntity> findByMascotaIdOrderByFechaDescIdDesc(long mascotaId);

}
