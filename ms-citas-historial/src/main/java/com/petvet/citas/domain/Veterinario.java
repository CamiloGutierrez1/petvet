package com.petvet.citas.domain;

import java.time.LocalTime;

/**
 * Veterinario del catálogo. Atiende todos los días de {@code horaInicio} a {@code horaFin}. Sin horario
 * propio, ambas horas son nulas y la agenda aplica el horario general de la clínica.
 */
public record Veterinario(Long id, String nombre, String especialidad, LocalTime horaInicio, LocalTime horaFin) {

    public Veterinario {
        if ((horaInicio == null) != (horaFin == null)) {
            throw new IllegalArgumentException("El horario del veterinario necesita hora de inicio y hora de fin");
        }
        if (horaInicio != null && !horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("La hora de inicio del veterinario debe ser anterior a la hora de fin");
        }
    }

    public Veterinario(Long id, String nombre, String especialidad) {
        this(id, nombre, especialidad, null, null);
    }

    public boolean tieneHorario() {
        return horaInicio != null;
    }

    /** El mismo veterinario con su horario, o con el indicado si no tiene uno propio. */
    public Veterinario conHorarioPorDefecto(LocalTime inicio, LocalTime fin) {
        return tieneHorario() ? this : new Veterinario(id, nombre, especialidad, inicio, fin);
    }
}
