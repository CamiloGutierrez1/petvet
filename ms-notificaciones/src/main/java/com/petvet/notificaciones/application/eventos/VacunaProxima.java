package com.petvet.notificaciones.application.eventos;

import java.time.LocalDate;

/** Evento vacuna.proxima. {@code duenoNombre} es opcional: hoy Citas no lo publica en este evento. */
public record VacunaProxima(
        Long mascotaId,
        String mascotaNombre,
        String duenoEmail,
        String vacuna,
        LocalDate fechaLimite,
        String duenoNombre) {

    public VacunaProxima {
        Campos.obligatorio(mascotaId, "mascotaId");
        Campos.obligatorio(mascotaNombre, "mascotaNombre");
        Campos.obligatorio(duenoEmail, "duenoEmail");
        Campos.obligatorio(vacuna, "vacuna");
        Campos.obligatorio(fechaLimite, "fechaLimite");
        duenoNombre = Campos.opcional(duenoNombre);
    }
}
