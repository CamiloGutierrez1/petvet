package com.petvet.citas.domain;

import static com.petvet.citas.domain.CalendarioVacunacionDemo.*;

import java.time.LocalDate;
import java.util.List;

/** Mascotas de 1 a 6 años: refuerzo anual de la vacuna combinada y de rabia. */
public final class EsquemaAdulto implements VacunacionStrategy {
    @Override
    public boolean aplica(Especie especie, int edadMeses) {
        return especieAdmitida(especie) && edadMeses >= 12 && edadMeses < 84;
    }

    @Override
    public List<VacunaPendiente> proximasVacunas(Mascota mascota, List<RegistroHistorial> historial,
                                               LocalDate hoy) {
        return ordenar(
                calcular(mascota, historial, vacunaCombinada(mascota.especie()), 12, 12),
                calcular(mascota, historial, RABIA, 12, 12));
    }
}
