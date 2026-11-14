package com.petvet.mascotas.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFotoMascotaRepository extends JpaRepository<FotoMascotaEntity, Long> {
}
