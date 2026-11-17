package com.petvet.citas.domain;

import static com.petvet.citas.domain.CalendarioVacunacionDemo.*;

import java.time.LocalDate;
import java.util.List;

/** Mascotas de menos de doce meses: primera dosis de la vacuna combinada a los 2 meses y de rabia a los 3. */
public final class EsquemaCachorro implements VacunacionStrategy {
    @Override
    public boolean aplica(Especie especie, int edadMeses) {
        return especieAdmitida(especie) && edadMeses >= 0 && edadMeses < 12;
    }

    @Override
    public List<VacunaPendiente> proximasVacunas(Mascota mascota, List<RegistroHistorial> historial,
                                               LocalDate hoy) {
        return ordenar(
                calcular(mascota, historial, vacunaCombinada(mascota.especie()), 2, 0),
                calcular(mascota, historial, RABIA, 3, 0));
    }
}
