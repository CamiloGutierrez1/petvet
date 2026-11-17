package com.petvet.citas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.petvet.citas.domain.Cita;
import com.petvet.citas.domain.CitaRepository;
import com.petvet.citas.domain.ConflictoException;
import com.petvet.citas.domain.Especie;
import com.petvet.citas.domain.EsquemaAdulto;
import com.petvet.citas.domain.EsquemaCachorro;
import com.petvet.citas.domain.EsquemaGeriatrico;
import com.petvet.citas.domain.EstadoCita;
import com.petvet.citas.domain.HistorialRepository;
import com.petvet.citas.domain.Mascota;
import com.petvet.citas.domain.MascotaClient;
import com.petvet.citas.domain.PublicadorEventos;
import com.petvet.citas.domain.RecursoNoEncontradoException;
import com.petvet.citas.domain.RegistroHistorial;
import com.petvet.citas.domain.SelectorEsquemaVacunacion;
import com.petvet.citas.domain.TipoConsulta;
import com.petvet.citas.domain.TipoServicio;
import com.petvet.citas.domain.VacunaPendiente;
import com.petvet.citas.domain.eventos.CitaCompletadaEvento;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrarConsultaUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T01:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);
    private static final Cita AGENDADA = new Cita(42L, 3L, 2L, HOY.atTime(19, 0),
            TipoServicio.CONSULTA_GENERAL, EstadoCita.AGENDADA, 60);
    private static final Mascota MASCOTA = new Mascota(3L, "Luna demo", Especie.GATO,
            LocalDate.of(2024, 5, 1), "Dueño demo", "dueno@example.test");

    @Mock private CitaRepository citas;
    @Mock private HistorialRepository historiales;
    @Mock private MascotaClient mascotas;
    @Mock private PublicadorEventos eventos;
    private final TransaccionesDePrueba transacciones = new TransaccionesDePrueba();
    private RegistrarConsultaUseCase registrar;

    @BeforeEach
    void prepararCasoDeUso() {
        var selector = new SelectorEsquemaVacunacion(List.of(
                new EsquemaCachorro(), new EsquemaAdulto(), new EsquemaGeriatrico()));
        registrar = new RegistrarConsultaUseCase(citas, historiales, mascotas, eventos, selector, CLOCK,
                transacciones);
    }

    @Test
    void unaCitaInexistenteNoConsultaMascotasNiAbreTransaccion() {
        when(citas.buscarPorId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrar.ejecutar(42L, datos("Diagnóstico demo")))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(historiales, mascotas, eventos);
        assertThat(transacciones.abiertas()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "CANCELADA"})
    void rechazaUnaCitaCerradaSinEfectosAdicionales(EstadoCita estado) {
        when(citas.buscarPorId(42L)).thenReturn(Optional.of(cita(estado)));

        assertThatThrownBy(() -> registrar.ejecutar(42L, datos("Diagnóstico demo")))
                .isInstanceOf(ConflictoException.class);

        verify(citas, never()).guardar(any());
        verifyNoInteractions(historiales, mascotas, eventos);
        assertThat(transacciones.abiertas()).isZero();
    }

    @Test
    void elBuilderRechazaElDiagnosticoVacioAntesDePersistirOCerrarLaCita() {
        when(citas.buscarPorId(42L)).thenReturn(Optional.of(AGENDADA));

        assertThatThrownBy(() -> registrar.ejecutar(42L, datos(" ")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(citas, never()).guardar(any());
        verifyNoInteractions(historiales, mascotas, eventos);
        assertThat(transacciones.abiertas()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "CANCELADA"})
    void siOtraSolicitudCierraLaCitaDuranteLaConsultaRestNoSeRegistraNada(EstadoCita estado) {
        when(citas.buscarPorId(42L)).thenReturn(Optional.of(AGENDADA));
        when(mascotas.buscar(3L)).thenReturn(MASCOTA);
        when(citas.buscarPorIdParaActualizar(42L)).thenReturn(Optional.of(cita(estado)));

        assertThatThrownBy(() -> registrar.ejecutar(42L, datos("Diagnóstico demo")))
                .isInstanceOf(ConflictoException.class);

        verify(citas, never()).guardar(any());
        verifyNoInteractions(historiales, eventos);
    }

    @Test
    void elEventoIncluyeLasVacunasCalculadasConLaConsultaRecienGuardada() {
        when(citas.buscarPorId(42L)).thenReturn(Optional.of(AGENDADA));
        when(citas.buscarPorIdParaActualizar(42L)).thenAnswer(invocacion -> {
            assertThat(transacciones.activa()).as("bloqueo dentro de la transacción").isTrue();
            return Optional.of(AGENDADA);
        });
        when(mascotas.buscar(3L)).thenAnswer(invocacion -> {
            assertThat(transacciones.activa()).as("consulta REST fuera de la transacción").isFalse();
            return MASCOTA;
        });
        AtomicReference<RegistroHistorial> persistido = new AtomicReference<>();
        when(historiales.guardar(any())).thenAnswer(invocacion -> {
            RegistroHistorial entrada = invocacion.getArgument(0);
            RegistroHistorial guardado = new RegistroHistorial(7L, entrada.citaId(), entrada.mascotaId(),
                    entrada.veterinarioId(), entrada.fecha(), entrada.tipoConsulta(), entrada.diagnostico(),
                    entrada.vacunasAplicadas(), entrada.tratamientos(), entrada.examenes(),
                    entrada.cirugias(), entrada.observaciones(), entrada.proximoControl());
            persistido.set(guardado);
            return guardado;
        });
        when(historiales.porMascota(3L)).thenAnswer(invocacion -> List.of(persistido.get()));

        RegistroHistorial resultado = registrar.ejecutar(42L, datos("Diagnóstico demo"));

        assertThat(resultado.id()).isEqualTo(7L);
        assertThat(resultado.fecha()).isEqualTo(LocalDateTime.of(2026, 11, 9, 20, 0));
        assertThat(resultado.cirugias()).containsExactly("Drenaje de absceso");
        ArgumentCaptor<Cita> completada = ArgumentCaptor.forClass(Cita.class);
        ArgumentCaptor<CitaCompletadaEvento> evento = ArgumentCaptor.forClass(CitaCompletadaEvento.class);
        InOrder orden = inOrder(historiales, citas, mascotas, eventos);
        orden.verify(citas).buscarPorId(42L);
        orden.verify(mascotas).buscar(3L);
        orden.verify(citas).buscarPorIdParaActualizar(42L);
        orden.verify(historiales).guardar(any());
        orden.verify(citas).guardar(completada.capture());
        orden.verify(historiales).porMascota(3L);
        orden.verify(eventos).citaCompletada(evento.capture());
        assertThat(transacciones.abiertas()).isEqualTo(1);
        assertThat(completada.getValue().estado()).isEqualTo(EstadoCita.COMPLETADA);
        assertThat(completada.getValue().duracionMinutos()).isEqualTo(60);
        assertThat(evento.getValue()).isEqualTo(new CitaCompletadaEvento(42L, "Luna demo", "Dueño demo",
                "dueno@example.test", "Diagnóstico demo", List.of("Rabia"), HOY.plusDays(14),
                List.of(new VacunaPendiente("Triple felina", LocalDate.of(2025, 5, 1)),
                        new VacunaPendiente("Rabia", HOY.plusMonths(12))),
                List.of("Hemograma", "Coprológico")));
    }

    @Test
    void sinExamenesElEventoLlevaUnaListaVacia() {
        when(citas.buscarPorId(42L)).thenReturn(Optional.of(AGENDADA));
        when(citas.buscarPorIdParaActualizar(42L)).thenReturn(Optional.of(AGENDADA));
        when(mascotas.buscar(3L)).thenReturn(MASCOTA);
        when(historiales.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        DatosConsulta sinExamenes = new DatosConsulta(TipoConsulta.GENERAL, "Diagnóstico demo",
                List.of(), List.of(), null, null, "Consulta académica", null);

        registrar.ejecutar(42L, sinExamenes);

        ArgumentCaptor<CitaCompletadaEvento> evento = ArgumentCaptor.forClass(CitaCompletadaEvento.class);
        verify(eventos).citaCompletada(evento.capture());
        assertThat(evento.getValue().examenes()).isEmpty();
    }

    private Cita cita(EstadoCita estado) {
        return new Cita(42L, 3L, 2L, AGENDADA.fechaHora(), AGENDADA.tipoServicio(), estado, 60);
    }

    private DatosConsulta datos(String diagnostico) {
        return new DatosConsulta(TipoConsulta.GENERAL, diagnostico, List.of("Rabia"),
                List.of(), List.of("Hemograma", "Coprológico"), List.of("Drenaje de absceso"),
                "Consulta académica", HOY.plusDays(14));
    }
}
