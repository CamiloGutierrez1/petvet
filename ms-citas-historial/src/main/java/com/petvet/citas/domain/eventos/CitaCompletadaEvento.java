package com.petvet.citas.domain.eventos;

import com.petvet.citas.domain.VacunaPendiente;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de la consulta para Notificaciones. {@code examenes} trae los nombres de los exámenes
 * registrados en la consulta, o una lista vacía; con ellos se arma el aviso de exámenes, sin un
 * evento aparte. {@code duenoNombre} sale de la ficha de Mascotas y Dueños, como en cita.agendada,
 * para que el correo salude al dueño por su nombre.
 */
public record CitaCompletadaEvento(
        long citaId,
        String mascotaNombre,
        String duenoNombre,
        String duenoEmail,
        String diagnostico,
        List<String> vacunasAplicadas,
        LocalDate proximoControl,
        List<VacunaPendiente> proximasVacunas,
        List<String> examenes) {

    public CitaCompletadaEvento {
        vacunasAplicadas = List.copyOf(vacunasAplicadas);
        proximasVacunas = List.copyOf(proximasVacunas);
        examenes = List.copyOf(examenes);
    }
}
