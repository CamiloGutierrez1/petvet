package com.petvet.citas.domain;

import java.time.LocalDate;
import java.util.List;

/** Calendario académico ficticio con un refuerzo de demostración cada doce meses. */
public final class EsquemaAdulto implements VacunacionStrategy {
    @Override
    public boolean aplica(Especie especie, int edadMeses) {
        return CalendarioVacunacionDemo.especieAdmitida(especie) && edadMeses >= 12 && edadMeses < 84;
    }

    @Override
    public List<VacunaPendiente> proximasVacunas(Mascota mascota, List<RegistroHistorial> historial,
                                               LocalDate hoy) {
        return CalendarioVacunacionDemo.calcular(mascota, historial, "Vacuna demo adulto", 12, 12);
    }
}
