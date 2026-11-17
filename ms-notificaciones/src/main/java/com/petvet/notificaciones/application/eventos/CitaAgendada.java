package com.petvet.notificaciones.application.eventos;

import java.time.LocalDateTime;

/**
 * Evento cita.agendada. {@code fechaHora} es la hora local de la clínica (America/Bogota), sin
 * desplazamiento, y se muestra tal cual. {@code tipoServicio} llega como nombre del enum de Citas.
 */
public record CitaAgendada(
        Long citaId,
        String mascotaNombre,
        String duenoNombre,
        String duenoEmail,
        String veterinarioNombre,
        LocalDateTime fechaHora,
        String tipoServicio) {

    public CitaAgendada {
        Campos.obligatorio(citaId, "citaId");
        Campos.obligatorio(mascotaNombre, "mascotaNombre");
        Campos.obligatorio(duenoEmail, "duenoEmail");
        Campos.obligatorio(veterinarioNombre, "veterinarioNombre");
        Campos.obligatorio(fechaHora, "fechaHora");
        Campos.obligatorio(tipoServicio, "tipoServicio");
        duenoNombre = Campos.opcional(duenoNombre);
    }
}
