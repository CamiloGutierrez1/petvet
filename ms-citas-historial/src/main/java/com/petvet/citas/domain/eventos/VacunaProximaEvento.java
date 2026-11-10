package com.petvet.citas.domain.eventos;

import java.time.LocalDate;

public record VacunaProximaEvento(
        long mascotaId,
        String mascotaNombre,
        String duenoEmail,
        String vacuna,
        LocalDate fechaLimite) {
}
