package com.petvet.citas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petvet.citas.domain.Cita;
import com.petvet.citas.domain.CitaRepository;
import com.petvet.citas.domain.EstadoCita;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import com.petvet.citas.domain.TipoServicio;
import com.petvet.citas.domain.Veterinario;
import com.petvet.citas.domain.VeterinarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
    private static final Cita CITA = new Cita(7L, 3L, 1L, FECHA.atTime(9, 0),
            TipoServicio.CONSULTA_GENERAL, EstadoCita.AGENDADA);

    @Mock private VeterinarioRepository veterinarios;
    @Mock private CitaRepository citas;
    private AgendaService agenda;

    @BeforeEach
    void prepararServicio() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneId.of("America/Bogota"));
        agenda = new AgendaService(veterinarios, citas, clock, "08:00", "18:00", 30);
    }

    @Test
    void buscaUnaCitaPorSuId() {
        when(citas.buscarPorId(7L)).thenReturn(Optional.of(CITA));

        assertThat(agenda.buscar(7L)).isEqualTo(CITA);
    }

    @Test
    void unaCitaInexistenteEsRecursoNoEncontrado() {
        when(citas.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agenda.buscar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La cita no existe");
    }

    @Test
    void sinFechaListaTodasLasCitasDelVeterinario() {
        Cita anterior = new Cita(5L, 4L, 1L, LocalDateTime.of(2026, 10, 2, 10, 0),
                TipoServicio.VACUNACION, EstadoCita.COMPLETADA);
        when(veterinarios.buscarPorId(1L)).thenReturn(Optional.of(veterinario()));
        when(citas.porVeterinario(1L)).thenReturn(List.of(CITA, anterior));

        assertThat(agenda.listar(null, 1L, null)).containsExactly(CITA, anterior);
    }

    @Test
    void conFechaSigueDevolviendoLaAgendaDelDia() {
        when(veterinarios.buscarPorId(1L)).thenReturn(Optional.of(veterinario()));
        when(citas.agenda(1L, FECHA)).thenReturn(List.of(CITA));

        assertThat(agenda.listar(null, 1L, FECHA)).containsExactly(CITA);
    }

    @Test
    void unVeterinarioInexistenteEsRecursoNoEncontradoTambienSinFecha() {
        when(veterinarios.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agenda.listar(null, 999L, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void rechazaCombinacionesDeFiltrosFueraDelContrato() {
        assertThatThrownBy(() -> agenda.listar(null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> agenda.listar(null, null, FECHA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> agenda.listar(3L, 1L, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> agenda.listar(3L, null, FECHA)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(citas, veterinarios);
    }

    @Test
    void laDisponibilidadSigueElHorarioDelVeterinario() {
        when(veterinarios.buscarPorId(3L)).thenReturn(Optional.of(cirujana()));
        when(citas.agenda(3L, FECHA)).thenReturn(List.of(new Cita(8L, 4L, 3L, FECHA.atTime(8, 0),
                TipoServicio.ESPECIALISTA, EstadoCita.AGENDADA)));

        List<LocalDateTime> horarios = agenda.disponibilidad(3L, FECHA).horarios();

        assertThat(horarios).hasSize(11).doesNotContain(FECHA.atTime(8, 0));
        assertThat(horarios.getFirst()).isEqualTo(FECHA.atTime(7, 0));
        assertThat(horarios.getLast()).isEqualTo(FECHA.atTime(12, 30));
    }

    @Test
    void unVeterinarioSinHorarioPropioAtiendeEnElHorarioGeneral() {
        when(veterinarios.buscarPorId(1L)).thenReturn(Optional.of(veterinario()));
        when(citas.agenda(1L, FECHA)).thenReturn(List.of());

        List<LocalDateTime> horarios = agenda.disponibilidad(1L, FECHA).horarios();

        assertThat(horarios).hasSize(20);
        assertThat(horarios.getFirst()).isEqualTo(FECHA.atTime(8, 0));
        assertThat(horarios.getLast()).isEqualTo(FECHA.atTime(17, 30));
    }

    @Test
    void elListadoCompletaElHorarioGeneralSoloDondeFalta() {
        when(veterinarios.listar()).thenReturn(List.of(veterinario(), cirujana()));

        assertThat(agenda.veterinarios()).containsExactly(
                new Veterinario(1L, "Dra. Paula Castaño", "Medicina general", LocalTime.of(8, 0), LocalTime.of(18, 0)),
                cirujana());
    }

    @Test
    void aceptaElPrimerYElUltimoTurnoDelHorarioDelVeterinario() {
        agenda.validarHorario(cirujana(), FECHA.atTime(7, 0));
        agenda.validarHorario(cirujana(), FECHA.atTime(12, 30));
    }

    @Test
    void rechazaHorasFueraDelHorarioDelVeterinarioNombrandoSuHorario() {
        for (LocalDateTime hora : List.of(FECHA.atTime(6, 30), FECHA.atTime(12, 45), FECHA.atTime(13, 0),
                FECHA.atTime(16, 0))) {
            assertThatThrownBy(() -> agenda.validarHorario(cirujana(), hora))
                    .as(hora.toString())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La cita debe quedar dentro del horario del veterinario, de 07:00 a 13:00");
        }
    }

    @Test
    void rechazaHorasQueNoCoincidenConElInicioDeUnTurno() {
        assertThatThrownBy(() -> agenda.validarHorario(cirujana(), FECHA.atTime(7, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La hora debe coincidir con el inicio de un turno: hay turnos cada 30 minutos desde las 07:00");
        assertThatThrownBy(() -> agenda.validarHorario(cirujana(), FECHA.atTime(9, 0, 30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sinHorarioPropioValidaContraElHorarioGeneral() {
        agenda.validarHorario(veterinario(), FECHA.atTime(17, 30));

        assertThatThrownBy(() -> agenda.validarHorario(veterinario(), FECHA.atTime(18, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cita debe quedar dentro del horario del veterinario, de 08:00 a 18:00");
    }

    @Test
    void rechazaUnaFechaPasadaAntesDeRevisarElHorario() {
        assertThatThrownBy(() -> agenda.validarHorario(cirujana(), LocalDateTime.of(2026, 11, 12, 9, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La cita debe tener una fecha y hora futura");
    }

    private static Veterinario veterinario() {
        return new Veterinario(1L, "Dra. Paula Castaño", "Medicina general");
    }

    private static Veterinario cirujana() {
        return new Veterinario(3L, "Dra. Valentina Ríos", "Cirugía", LocalTime.of(7, 0), LocalTime.of(13, 0));
    }
}
