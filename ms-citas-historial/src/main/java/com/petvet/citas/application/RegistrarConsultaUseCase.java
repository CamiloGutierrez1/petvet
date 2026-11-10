package com.petvet.citas.application;

import com.petvet.citas.domain.*;
import com.petvet.citas.domain.eventos.CitaCompletadaEvento;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class RegistrarConsultaUseCase {
    private final CitaRepository citas;
    private final HistorialRepository historiales;
    private final MascotaClient mascotas;
    private final PublicadorEventos eventos;
    private final SelectorEsquemaVacunacion selector;
    private final Clock clock;
    private final TransactionOperations transacciones;

    public RegistrarConsultaUseCase(CitaRepository citas, HistorialRepository historiales,
            MascotaClient mascotas, PublicadorEventos eventos, SelectorEsquemaVacunacion selector,
            Clock clock, TransactionOperations transacciones) {
        this.citas = citas;
        this.historiales = historiales;
        this.mascotas = mascotas;
        this.eventos = eventos;
        this.selector = selector;
        this.clock = clock;
        this.transacciones = transacciones;
    }

    /**
     * Valida y consulta la ficha en Mascotas sin transacción ni bloqueo. Después bloquea la cita,
     * confirma que sigue agendada y guarda el historial, el cambio de estado y el evento juntos.
     */
    public RegistroHistorial ejecutar(long citaId, DatosConsulta datos) {
        Cita cita = citas.buscarPorId(citaId).orElseThrow(RegistrarConsultaUseCase::citaInexistente);
        exigirAgendada(cita);
        LocalDateTime ahora = LocalDateTime.now(clock);
        RegistroHistorial borrador = construir(cita, datos, ahora);
        Mascota mascota = mascotas.buscar(cita.mascotaId());
        return transacciones.execute(estado -> completar(citaId, borrador, mascota, ahora.toLocalDate()));
    }

    private RegistroHistorial construir(Cita cita, DatosConsulta datos, LocalDateTime ahora) {
        if (datos.tipoConsulta() == null) {
            throw new IllegalArgumentException("El tipo de consulta es obligatorio");
        }
        if (datos.proximoControl() != null && (datos.proximoControl().isBefore(ahora.toLocalDate())
                || datos.proximoControl().getYear() > 9999)) {
            throw new IllegalArgumentException("El próximo control debe estar entre hoy y el año 9999");
        }
        var builder = new RegistroHistorialBuilder(cita.id(), cita.mascotaId(), cita.veterinarioId(), ahora)
                .diagnostico(datos.diagnostico())
                .vacunasAplicadas(datos.vacunasAplicadas())
                .tratamientos(datos.tratamientos())
                .examenes(datos.examenes())
                .observaciones(datos.observaciones())
                .proximoControl(datos.proximoControl());
        Consulta consulta = switch (datos.tipoConsulta()) {
            case GENERAL -> new ConsultaGeneral();
            case ESPECIALISTA -> new ConsultaEspecialista();
        };
        return consulta.registrar(builder);
    }

    private RegistroHistorial completar(long citaId, RegistroHistorial borrador, Mascota mascota, LocalDate hoy) {
        Cita cita = citas.buscarPorIdParaActualizar(citaId)
                .orElseThrow(RegistrarConsultaUseCase::citaInexistente);
        // Otra solicitud pudo cerrar la cita mientras se consultaba la ficha de la mascota.
        exigirAgendada(cita);
        RegistroHistorial registro = historiales.guardar(borrador);
        citas.guardar(new Cita(cita.id(), cita.mascotaId(), cita.veterinarioId(), cita.fechaHora(),
                cita.tipoServicio(), EstadoCita.COMPLETADA, cita.duracionMinutos()));
        var vacunas = selector.proximasVacunas(mascota, historiales.porMascota(cita.mascotaId()), hoy);
        eventos.citaCompletada(new CitaCompletadaEvento(cita.id(), mascota.nombre(), mascota.duenoEmail(),
                registro.diagnostico(), registro.vacunasAplicadas(), registro.proximoControl(), vacunas));
        return registro;
    }

    private static void exigirAgendada(Cita cita) {
        if (cita.estado() != EstadoCita.AGENDADA) {
            throw new ConflictoException("Solo puede registrarse la consulta de una cita agendada");
        }
    }

    private static RecursoNoEncontradoException citaInexistente() {
        return new RecursoNoEncontradoException("La cita no existe");
    }
}
