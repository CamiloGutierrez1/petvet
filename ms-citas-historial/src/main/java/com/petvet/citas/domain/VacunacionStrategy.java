package com.petvet.citas.domain;

import java.time.LocalDate;
import java.util.List;

public interface VacunacionStrategy {
    boolean aplica(Especie especie, int edadMeses);

    List<VacunaPendiente> proximasVacunas(Mascota mascota, List<RegistroHistorial> historial,
                                        LocalDate hoy);
}
