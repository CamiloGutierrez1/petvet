package com.petvet.citas.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Reglas ficticias de calendario para demostrar Strategy en el proyecto académico. */
final class CalendarioVacunacionDemo {

    private CalendarioVacunacionDemo() {
    }

    static boolean especieAdmitida(Especie especie) {
        return especie == Especie.PERRO || especie == Especie.GATO;
    }

    static List<VacunaPendiente> calcular(Mascota mascota, List<RegistroHistorial> historial,
                                         String vacuna, int primeraEdadMeses, int refuerzoMeses) {
        Optional<LocalDate> ultimaAplicacion = historial.stream()
                .filter(registro -> Long.valueOf(registro.mascotaId()).equals(mascota.id()))
                .filter(registro -> registro.vacunasAplicadas().stream()
                        .anyMatch(aplicada -> vacuna.equalsIgnoreCase(aplicada)))
                .map(registro -> registro.fecha().toLocalDate())
                .max(Comparator.naturalOrder());

        if (ultimaAplicacion.isPresent() && refuerzoMeses == 0) {
            return List.of();
        }
        LocalDate fechaLimite = ultimaAplicacion
                .map(fecha -> fecha.plusMonths(refuerzoMeses))
                .orElseGet(() -> mascota.fechaNacimiento().plusMonths(primeraEdadMeses));
        return List.of(new VacunaPendiente(vacuna, fechaLimite));
    }
}
