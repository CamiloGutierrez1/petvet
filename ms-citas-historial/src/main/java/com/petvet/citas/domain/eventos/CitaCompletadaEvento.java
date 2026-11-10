package com.petvet.citas.domain.eventos;

import com.petvet.citas.domain.VacunaPendiente;
import java.time.LocalDate;
import java.util.List;

public record CitaCompletadaEvento(
        long citaId,
        String mascotaNombre,
        String duenoEmail,
        String diagnostico,
        List<String> vacunasAplicadas,
        LocalDate proximoControl,
        List<VacunaPendiente> proximasVacunas) {

    public CitaCompletadaEvento {
        vacunasAplicadas = List.copyOf(vacunasAplicadas);
        proximasVacunas = List.copyOf(proximasVacunas);
    }
}
