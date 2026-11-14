package com.petvet.mascotas.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataDuenoRepository extends JpaRepository<DuenoEntity, Long> {
    List<DuenoEntity> findAllByOrderByIdAsc();

    Optional<DuenoEntity> findByEmail(String email);
}
