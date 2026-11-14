package com.petvet.citas.domain.eventos;

import java.time.LocalDate;

/** Recordatorio de una vacuna. {@code duenoNombre} sale de la ficha, como en los eventos de cita. */
public record VacunaProximaEvento(
        long mascotaId,
        String mascotaNombre,
        String duenoNombre,
        String duenoEmail,
        String vacuna,
        LocalDate fechaLimite) {
}
