package com.petvet.notificaciones.application.eventos;

import java.time.LocalDate;
import java.util.List;

/**
 * Evento cita.completada. {@code examenes} y {@code duenoNombre} son opcionales: los eventos que ya
 * estaban en la outbox de Citas no los traen. Las listas ausentes quedan vacías y
 * {@code proximoControl} puede ser nulo.
 */
public record CitaCompletada(
        Long citaId,
        String mascotaNombre,
        String duenoEmail,
        String diagnostico,
        List<String> vacunasAplicadas,
        LocalDate proximoControl,
        List<VacunaPendiente> proximasVacunas,
        List<String> examenes,
        String duenoNombre) {

    public CitaCompletada {
        Campos.obligatorio(citaId, "citaId");
        Campos.obligatorio(mascotaNombre, "mascotaNombre");
        Campos.obligatorio(duenoEmail, "duenoEmail");
        Campos.obligatorio(diagnostico, "diagnostico");
        vacunasAplicadas = Campos.lista(vacunasAplicadas);
        proximasVacunas = Campos.lista(proximasVacunas);
        examenes = Campos.lista(examenes);
        duenoNombre = Campos.opcional(duenoNombre);
    }
}
