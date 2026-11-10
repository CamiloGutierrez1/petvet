package com.petvet.citas.domain.eventos;

import java.time.LocalDateTime;

public record CitaAgendadaEvento(
        long citaId,
        String mascotaNombre,
        String duenoNombre,
        String duenoEmail,
        String veterinarioNombre,
        LocalDateTime fechaHora,
        String tipoServicio) {
}
