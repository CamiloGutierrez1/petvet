package com.petvet.citas.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Cita(Long id, long mascotaId, long veterinarioId, LocalDateTime fechaHora,
                   TipoServicio tipoServicio, EstadoCita estado, int duracionMinutos) {

    public Cita(Long id, long mascotaId, long veterinarioId, LocalDateTime fechaHora,
                TipoServicio tipoServicio, EstadoCita estado) {
        this(id, mascotaId, veterinarioId, fechaHora, tipoServicio, estado, 30);
    }

    public Cita {
        if (mascotaId <= 0 || veterinarioId <= 0) {
            throw new IllegalArgumentException("Los identificadores de mascota y veterinario deben ser positivos");
        }
        Objects.requireNonNull(fechaHora, "La fecha de la cita es obligatoria");
        Objects.requireNonNull(tipoServicio, "El tipo de servicio es obligatorio");
        Objects.requireNonNull(estado, "El estado de la cita es obligatorio");
        if (duracionMinutos < 1 || duracionMinutos > 480) {
            throw new IllegalArgumentException("La duración de la cita debe estar entre 1 y 480 minutos");
        }
    }
}
