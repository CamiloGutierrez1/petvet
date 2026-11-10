package com.petvet.citas.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataVeterinarioRepository extends JpaRepository<VeterinarioEntity, Long> {
    List<VeterinarioEntity> findAllByOrderByIdAsc();
}
