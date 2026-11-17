package com.petvet.citas.domain;

import static com.petvet.citas.domain.CalendarioVacunacionDemo.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Mascotas de 7 años en adelante: rabia sigue siendo anual y la vacuna combinada se refuerza cada
 * tres años para no sobrevacunar a una mascota mayor.
 */
public final class EsquemaGeriatrico implements VacunacionStrategy {
    @Override
    public boolean aplica(Especie especie, int edadMeses) {
        return especieAdmitida(especie) && edadMeses >= 84;
    }

    @Override
    public List<VacunaPendiente> proximasVacunas(Mascota mascota, List<RegistroHistorial> historial,
                                               LocalDate hoy) {
        return ordenar(
                calcular(mascota, historial, vacunaCombinada(mascota.especie()), 84, 36),
                calcular(mascota, historial, RABIA, 84, 12));
    }
}
