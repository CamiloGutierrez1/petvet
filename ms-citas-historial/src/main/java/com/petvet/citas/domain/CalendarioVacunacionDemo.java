package com.petvet.citas.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reglas simplificadas de calendario para demostrar Strategy en el proyecto académico. Usa los
 * nombres de vacunas que se registran en la consulta, pero no reemplaza el criterio del veterinario.
 */
final class CalendarioVacunacionDemo {

    static final String RABIA = "Rabia";
    static final String POLIVALENTE = "Polivalente";
    static final String TRIPLE_FELINA = "Triple felina";

    private CalendarioVacunacionDemo() {
    }

    static boolean especieAdmitida(Especie especie) {
        return especie == Especie.PERRO || especie == Especie.GATO;
    }

    /** Vacuna combinada de cada especie: polivalente en perros y triple felina en gatos. */
    static String vacunaCombinada(Especie especie) {
        return especie == Especie.GATO ? TRIPLE_FELINA : POLIVALENTE;
    }

    /** Junta las vacunas de un esquema en orden de vencimiento. */
    @SafeVarargs
    static List<VacunaPendiente> ordenar(List<VacunaPendiente>... pendientes) {
        return Stream.of(pendientes)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(VacunaPendiente::fechaLimite).thenComparing(VacunaPendiente::vacuna))
                .toList();
    }

    static List<VacunaPendiente> calcular(Mascota mascota, List<RegistroHistorial> historial,
                                         String vacuna, int primeraEdadMeses, int refuerzoMeses) {
        Optional<LocalDate> ultimaAplicacion = historial.stream()
                .filter(registro -> Long.valueOf(registro.mascotaId()).equals(mascota.id()))
                .filter(registro -> registro.vacunasAplicadas().stream()
                        .anyMatch(aplicada -> vacuna.equalsIgnoreCase(aplicada.strip())))
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
