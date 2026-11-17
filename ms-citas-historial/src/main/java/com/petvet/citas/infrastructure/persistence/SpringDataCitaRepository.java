package com.petvet.citas.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataCitaRepository extends JpaRepository<CitaEntity, Long> {
    List<CitaEntity> findByMascotaIdOrderByFechaHoraDescIdDesc(long mascotaId);

    List<CitaEntity> findByVeterinarioIdOrderByFechaHoraDescIdDesc(long veterinarioId);

    @Query("select distinct cita.mascotaId from CitaEntity cita order by cita.mascotaId")
    List<Long> mascotasConCitas();

    List<CitaEntity> findByVeterinarioIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAscIdAsc(
            long veterinarioId, LocalDateTime inicio, LocalDateTime fin);

    // Misma expresión de intervalo que las restricciones de exclusión de V3, para aprovechar su índice GiST.
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM citas
                WHERE veterinario_id = :veterinarioId
                  AND estado <> 'CANCELADA'
                  AND tsrange(fecha_hora, fecha_hora + duracion_minutos * INTERVAL '1 minute', '[)')
                      && tsrange(CAST(:inicio AS timestamp), CAST(:fin AS timestamp), '[)'))
            """, nativeQuery = true)
    boolean veterinarioOcupado(@Param("veterinarioId") long veterinarioId,
            @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM citas
                WHERE mascota_id = :mascotaId
                  AND estado <> 'CANCELADA'
                  AND tsrange(fecha_hora, fecha_hora + duracion_minutos * INTERVAL '1 minute', '[)')
                      && tsrange(CAST(:inicio AS timestamp), CAST(:fin AS timestamp), '[)'))
            """, nativeQuery = true)
    boolean mascotaOcupada(@Param("mascotaId") long mascotaId,
            @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cita from CitaEntity cita where cita.id = :id")
    Optional<CitaEntity> buscarParaActualizar(@Param("id") long id);
}
