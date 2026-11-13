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

    private static Veterinario veterinario() {
        return new Veterinario(1L, "Dra. Paula Castaño", "Medicina general");
    }
}
