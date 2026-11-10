package com.petvet.citas.domain;

import java.time.LocalDate;
import java.util.List;

/** Calendario académico ficticio para mascotas de menos de doce meses. */
public final class EsquemaCachorro implements VacunacionStrategy {
    @Override
    public boolean aplica(Especie especie, int edadMeses) {
        return CalendarioVacunacionDemo.especieAdmitida(especie) && edadMeses >= 0 && edadMeses < 12;
    }

    @Override
    public List<VacunaPendiente> proximasVacunas(Mascota mascota, List<RegistroHistorial> historial,
                                               LocalDate hoy) {
        return CalendarioVacunacionDemo.calcular(mascota, historial, "Vacuna demo cachorro", 2, 0);
    }
}
