package com.petvet.citas.domain;

import com.petvet.citas.domain.eventos.CitaAgendadaEvento;
import com.petvet.citas.domain.eventos.CitaCompletadaEvento;
import com.petvet.citas.domain.eventos.VacunaProximaEvento;

/** Registra los eventos del negocio junto con los cambios que los originaron. */
public interface PublicadorEventos {
    void citaAgendada(CitaAgendadaEvento evento);

    void citaCompletada(CitaCompletadaEvento evento);

    void vacunaProxima(VacunaProximaEvento evento);
}
