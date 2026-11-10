package com.petvet.citas.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public final class SelectorEsquemaVacunacion {

    private final List<VacunacionStrategy> esquemas;

    public SelectorEsquemaVacunacion(List<VacunacionStrategy> esquemas) {
        this.esquemas = List.copyOf(esquemas);
    }

    public List<VacunaPendiente> proximasVacunas(Mascota mascota, List<RegistroHistorial> historial,
                                               LocalDate hoy) {
        Objects.requireNonNull(mascota, "La mascota es obligatoria");
        Objects.requireNonNull(hoy, "La fecha actual es obligatoria");
        if (mascota.fechaNacimiento().isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede estar en el futuro");
        }
        int edadMeses = Math.toIntExact(ChronoUnit.MONTHS.between(mascota.fechaNacimiento(), hoy));
        return esquemas.stream()
                .filter(esquema -> esquema.aplica(mascota.especie(), edadMeses))
                .findFirst()
                .map(esquema -> List.copyOf(esquema.proximasVacunas(mascota, historial, hoy)))
                .orElseGet(List::of);
    }
}
