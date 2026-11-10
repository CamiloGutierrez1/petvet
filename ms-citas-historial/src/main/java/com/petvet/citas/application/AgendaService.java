package com.petvet.citas.application;

import com.petvet.citas.domain.*;
import java.time.*;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaService {
    private final VeterinarioRepository veterinarios;
    private final CitaRepository citas;
    private final Clock clock;
    private final LocalTime apertura;
    private final LocalTime cierre;
    private final int duracionMinutos;

    public AgendaService(VeterinarioRepository veterinarios, CitaRepository citas, Clock clock,
            @Value("${petvet.agenda.apertura}") String apertura,
            @Value("${petvet.agenda.cierre}") String cierre,
            @Value("${petvet.agenda.duracion-minutos}") int duracionMinutos) {
        this.veterinarios = veterinarios;
        this.citas = citas;
        this.clock = clock;
        this.apertura = LocalTime.parse(apertura);
        this.cierre = LocalTime.parse(cierre);
        this.duracionMinutos = duracionMinutos;
        long minutos = Duration.between(this.apertura, this.cierre).toMinutes();
        if (duracionMinutos <= 0 || duracionMinutos > 480 || minutos <= 0 || minutos % duracionMinutos != 0
                || this.apertura.getSecond() != 0 || this.cierre.getSecond() != 0
                || this.apertura.getNano() != 0 || this.cierre.getNano() != 0) {
            throw new IllegalArgumentException("El horario debe contener turnos completos y una duración positiva");
        }
    }

    public int duracionMinutos() {
        return duracionMinutos;
    }

    public List<Veterinario> veterinarios() {
        return veterinarios.listar();
    }

    public Veterinario veterinario(long id) {
        return veterinarios.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El veterinario no existe"));
    }

    public void validarHorario(LocalDateTime fechaHora) {
        if (fechaHora == null || !fechaHora.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("La cita debe tener una fecha y hora futura");
        }
        validarFecha(fechaHora.toLocalDate());
        LocalDateTime inicio = fechaHora.toLocalDate().atTime(apertura);
        LocalDateTime fin = fechaHora.toLocalDate().atTime(cierre);
        long minutos = Duration.between(inicio, fechaHora).toMinutes();
        if (fechaHora.isBefore(inicio) || fechaHora.plusMinutes(duracionMinutos).isAfter(fin)
                || fechaHora.getSecond() != 0 || fechaHora.getNano() != 0 || minutos % duracionMinutos != 0) {
            throw new IllegalArgumentException("La hora debe coincidir con un turno disponible dentro del horario de atención");
        }
    }

    @Transactional(readOnly = true)
    public Disponibilidad disponibilidad(long veterinarioId, LocalDate fecha) {
        validarFecha(fecha);
        veterinario(veterinarioId);
        if (fecha.isBefore(LocalDate.now(clock))) {
            return new Disponibilidad(veterinarioId, fecha, List.of());
        }
        var ocupadas = citas.agenda(veterinarioId, fecha).stream()
                .filter(cita -> cita.estado() != EstadoCita.CANCELADA).toList();
        int turnos = (int) Duration.between(apertura, cierre).toMinutes() / duracionMinutos;
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<LocalDateTime> horarios = IntStream.range(0, turnos)
                .mapToObj(turno -> fecha.atTime(apertura).plusMinutes((long) turno * duracionMinutos))
                .filter(hora -> hora.isAfter(ahora))
                .filter(hora -> ocupadas.stream().noneMatch(cita ->
                        hora.isBefore(cita.fechaHora().plusMinutes(cita.duracionMinutos()))
                                && cita.fechaHora().isBefore(hora.plusMinutes(duracionMinutos))))
                .toList();
        return new Disponibilidad(veterinarioId, fecha, horarios);
    }

    @Transactional(readOnly = true)
    public List<Cita> listar(Long mascotaId, Long veterinarioId, LocalDate fecha) {
        if (mascotaId != null && veterinarioId == null && fecha == null) {
            return citas.porMascota(mascotaId);
        }
        if (mascotaId == null && veterinarioId != null && fecha != null) {
            validarFecha(fecha);
            veterinario(veterinarioId);
            return citas.agenda(veterinarioId, fecha);
        }
        throw new IllegalArgumentException("Use mascotaId o la combinación veterinarioId y fecha");
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.getYear() < 1 || fecha.getYear() > 9999) {
            throw new IllegalArgumentException("La fecha debe usar un año entre 0001 y 9999");
        }
    }

    @Transactional
    public Cita cancelar(long id) {
        Cita cita = citas.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cita no existe"));
        if (cita.estado() == EstadoCita.COMPLETADA) {
            throw new ConflictoException("Una cita completada no puede cancelarse");
        }
        if (cita.estado() == EstadoCita.CANCELADA) {
            return cita;
        }
        return citas.guardar(new Cita(cita.id(), cita.mascotaId(), cita.veterinarioId(),
                cita.fechaHora(), cita.tipoServicio(), EstadoCita.CANCELADA, cita.duracionMinutos()));
    }

    public record Disponibilidad(long veterinarioId, LocalDate fecha, List<LocalDateTime> horarios) { }
}
