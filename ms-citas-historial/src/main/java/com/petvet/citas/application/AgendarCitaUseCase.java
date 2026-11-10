package com.petvet.citas.application;

import com.petvet.citas.domain.*;
import com.petvet.citas.domain.eventos.CitaAgendadaEvento;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class AgendarCitaUseCase {
    private final CitaRepository citas;
    private final MascotaClient mascotas;
    private final AgendaService agenda;
    private final PublicadorEventos eventos;
    private final TransactionOperations transacciones;

    public AgendarCitaUseCase(CitaRepository citas, MascotaClient mascotas, AgendaService agenda,
            PublicadorEventos eventos, TransactionOperations transacciones) {
        this.citas = citas;
        this.mascotas = mascotas;
        this.agenda = agenda;
        this.eventos = eventos;
        this.transacciones = transacciones;
    }

    /**
     * Las validaciones y la consulta REST a Mascotas ocurren antes de abrir la transacción, para
     * que una dependencia lenta no retenga una conexión a la base. La cita y su evento se guardan
     * juntos. Si otra solicitud gana el turno entre la revisión y la escritura, PostgreSQL la rechaza.
     */
    public Cita ejecutar(long mascotaId, long veterinarioId, LocalDateTime fechaHora, TipoServicio tipoServicio) {
        if (mascotaId <= 0 || veterinarioId <= 0 || tipoServicio == null) {
            throw new IllegalArgumentException("La mascota, el veterinario y el tipo de servicio son obligatorios");
        }
        agenda.validarHorario(fechaHora);
        Veterinario veterinario = agenda.veterinario(veterinarioId);
        int duracionMinutos = agenda.duracionMinutos();
        if (citas.horarioOcupado(veterinarioId, fechaHora, duracionMinutos)) {
            throw new ConflictoException("El veterinario ya tiene una cita en ese horario");
        }
        if (citas.mascotaOcupada(mascotaId, fechaHora, duracionMinutos)) {
            throw new ConflictoException("La mascota ya tiene una cita en ese horario");
        }
        Mascota mascota = mascotas.buscar(mascotaId);
        Cita nueva = new Cita(null, mascotaId, veterinarioId, fechaHora, tipoServicio,
                EstadoCita.AGENDADA, duracionMinutos);
        return transacciones.execute(estado -> {
            Cita cita = citas.guardar(nueva);
            eventos.citaAgendada(new CitaAgendadaEvento(cita.id(), mascota.nombre(), mascota.duenoNombre(),
                    mascota.duenoEmail(), veterinario.nombre(), fechaHora, tipoServicio.name()));
            return cita;
        });
    }
}
