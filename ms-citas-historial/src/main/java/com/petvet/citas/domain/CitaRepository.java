package com.petvet.citas.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository {
    Cita guardar(Cita cita);

    Optional<Cita> buscarPorId(long id);

    Optional<Cita> buscarPorIdParaActualizar(long id);

    List<Cita> porMascota(long mascotaId);

    List<Cita> agenda(long veterinarioId, LocalDate fecha);

    List<Long> mascotasConCitas();

    /** Indica si el veterinario tiene una cita vigente que se cruza con el turno solicitado. */
    boolean horarioOcupado(long veterinarioId, LocalDateTime inicio, int duracionMinutos);

    /** Indica si la mascota tiene una cita vigente que se cruza con el turno solicitado. */
    boolean mascotaOcupada(long mascotaId, LocalDateTime inicio, int duracionMinutos);
}
