package com.petvet.citas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petvet.citas.domain.Cita;
import com.petvet.citas.domain.CitaRepository;
import com.petvet.citas.domain.ConflictoException;
import com.petvet.citas.domain.Especie;
import com.petvet.citas.domain.EstadoCita;
import com.petvet.citas.domain.Mascota;
import com.petvet.citas.domain.MascotaClient;
import com.petvet.citas.domain.PublicadorEventos;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import com.petvet.citas.domain.TipoServicio;
import com.petvet.citas.domain.Veterinario;
import com.petvet.citas.domain.eventos.CitaAgendadaEvento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgendarCitaUseCaseTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 20, 9, 30);
    private static final Veterinario VETERINARIO = new Veterinario(2L, "Veterinario demo", "General",
            LocalTime.of(7, 0), LocalTime.of(13, 0));
    private static final Mascota MASCOTA = new Mascota(3L, "Luna demo", Especie.GATO,
            LocalDate.of(2024, 5, 1), "Dueño demo", "dueno@example.test");

    @Mock private CitaRepository citas;
    @Mock private MascotaClient mascotas;
    @Mock private AgendaService agenda;
    @Mock private PublicadorEventos eventos;
    private final TransaccionesDePrueba transacciones = new TransaccionesDePrueba();
    private AgendarCitaUseCase agendar;

    @BeforeEach
    void prepararCasoDeUso() {
        agendar = new AgendarCitaUseCase(citas, mascotas, agenda, eventos, transacciones);
        when(agenda.veterinario(2L)).thenReturn(VETERINARIO);
        lenient().when(agenda.duracionMinutos()).thenReturn(30);
    }

    @Test
    void rechazaElHorarioOcupadoAntesDeConsultarMascotasOEscribir() {
        when(citas.horarioOcupado(2L, FECHA, 30)).thenReturn(true);

        assertThatThrownBy(() -> agendar.ejecutar(3L, 2L, FECHA, TipoServicio.VACUNACION))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("veterinario");

        InOrder orden = inOrder(agenda);
        orden.verify(agenda).veterinario(2L);
        orden.verify(agenda).validarHorario(VETERINARIO, FECHA);
        verify(citas, never()).guardar(any());
        verifyNoInteractions(mascotas, eventos);
        assertThat(transacciones.abiertas()).isZero();
    }

    @Test
    void unaHoraFueraDelHorarioDelVeterinarioNoRevisaTurnosNiEscribe() {
        doThrow(new IllegalArgumentException("La cita debe quedar dentro del horario del veterinario, de 07:00 a 13:00"))
                .when(agenda).validarHorario(VETERINARIO, FECHA);

        assertThatThrownBy(() -> agendar.ejecutar(3L, 2L, FECHA, TipoServicio.CONSULTA_GENERAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("07:00 a 13:00");

        verifyNoInteractions(citas, mascotas, eventos);
        assertThat(transacciones.abiertas()).isZero();
    }

    @Test
    void impideQueLaMascotaReserveConDosVeterinariosAlMismoTiempo() {
        when(citas.mascotaOcupada(3L, FECHA, 30)).thenReturn(true);

        assertThatThrownBy(() -> agendar.ejecutar(3L, 2L, FECHA, TipoServicio.CONTROL))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("mascota");

        verify(citas, never()).guardar(any());
        verifyNoInteractions(mascotas, eventos);
        assertThat(transacciones.abiertas()).isZero();
    }

    @Test
    void unaMascotaInexistenteNoGeneraCitaNiEvento() {
        when(mascotas.buscar(3L)).thenThrow(new RecursoNoEncontradoException("La mascota no existe"));

        assertThatThrownBy(() -> agendar.ejecutar(3L, 2L, FECHA, TipoServicio.CONSULTA_GENERAL))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(citas, never()).guardar(any());
        verifyNoInteractions(eventos);
        assertThat(transacciones.abiertas()).isZero();
    }

    @Test
    void guardaLaCitaConDuracionConfiguradaYPublicaSuIdentificadorPersistido() {
        when(mascotas.buscar(3L)).thenReturn(MASCOTA);
        when(citas.guardar(any())).thenAnswer(invocacion -> {
            Cita entrada = invocacion.getArgument(0);
            return new Cita(42L, entrada.mascotaId(), entrada.veterinarioId(), entrada.fechaHora(),
                    entrada.tipoServicio(), entrada.estado(), entrada.duracionMinutos());
        });

        Cita resultado = agendar.ejecutar(3L, 2L, FECHA, TipoServicio.VACUNACION);

        ArgumentCaptor<Cita> citaGuardada = ArgumentCaptor.forClass(Cita.class);
        verify(citas).guardar(citaGuardada.capture());
        assertThat(citaGuardada.getValue()).isEqualTo(new Cita(null, 3L, 2L, FECHA,
                TipoServicio.VACUNACION, EstadoCita.AGENDADA, 30));
        assertThat(resultado.id()).isEqualTo(42L);
        ArgumentCaptor<CitaAgendadaEvento> evento = ArgumentCaptor.forClass(CitaAgendadaEvento.class);
        verify(eventos).citaAgendada(evento.capture());
        assertThat(evento.getValue()).isEqualTo(new CitaAgendadaEvento(42L, "Luna demo", "Dueño demo",
                "dueno@example.test", "Veterinario demo", FECHA, "VACUNACION"));
    }

    @Test
    void consultaMascotasSinTransaccionYGuardaLaCitaConSuEventoEnUnaSola() {
        when(mascotas.buscar(3L)).thenAnswer(invocacion -> {
            assertThat(transacciones.activa()).as("consulta REST fuera de la transacción").isFalse();
            return MASCOTA;
        });
        when(citas.guardar(any())).thenAnswer(invocacion -> {
            assertThat(transacciones.activa()).as("cita dentro de la transacción").isTrue();
            Cita entrada = invocacion.getArgument(0);
            return new Cita(42L, entrada.mascotaId(), entrada.veterinarioId(), entrada.fechaHora(),
                    entrada.tipoServicio(), entrada.estado(), entrada.duracionMinutos());
        });
        doAnswer(invocacion -> {
            assertThat(transacciones.activa()).as("evento dentro de la transacción").isTrue();
            return null;
        }).when(eventos).citaAgendada(any());

        agendar.ejecutar(3L, 2L, FECHA, TipoServicio.CONTROL);

        verify(eventos).citaAgendada(any());
        assertThat(transacciones.abiertas()).isEqualTo(1);
    }
}
