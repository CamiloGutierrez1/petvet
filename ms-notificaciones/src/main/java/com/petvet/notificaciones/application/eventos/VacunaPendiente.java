package com.petvet.notificaciones.application.eventos;

import java.time.LocalDate;

/** Vacuna que le toca a la mascota y su fecha límite, que puede estar vencida. */
public record VacunaPendiente(String vacuna, LocalDate fechaLimite) {

    public VacunaPendiente {
        Campos.obligatorio(vacuna, "proximasVacunas.vacuna");
        Campos.obligatorio(fechaLimite, "proximasVacunas.fechaLimite");
    }
}
