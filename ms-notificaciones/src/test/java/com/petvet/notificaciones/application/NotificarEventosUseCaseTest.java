package com.petvet.notificaciones.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petvet.notificaciones.application.eventos.CitaAgendada;
import com.petvet.notificaciones.application.eventos.CitaCompletada;
import com.petvet.notificaciones.application.eventos.VacunaPendiente;
import com.petvet.notificaciones.application.eventos.VacunaProxima;
import com.petvet.notificaciones.domain.EnviadorNotificaciones;
import com.petvet.notificaciones.domain.EnvioNoDisponibleException;
import com.petvet.notificaciones.domain.Notificacion;
import com.petvet.notificaciones.domain.NotificacionRepository;
import com.petvet.notificaciones.domain.TipoNotificacion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificarEventosUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-13T17:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneId.of("America/Bogota"));
    private static final String MESSAGE_ID = "2f02c4a9-80f9-4fd4-9652-cd701a50655e";
    private static final CitaAgendada AGENDADA = new CitaAgendada(7L, "Tobby", "Laura Méndez",
            "laura.mendez@correo.com", "Dra. Paula Castaño", LocalDateTime.of(2026, 11, 20, 9, 30), "VACUNACION");
    private static final CitaCompletada COMPLETADA = new CitaCompletada(7L, "Tobby", "laura.mendez@correo.com",
            "Otitis leve", List.of("Rabia"), LocalDate.of(2026, 12, 4),
            List.of(new VacunaPendiente("Polivalente", LocalDate.of(2026, 12, 13))), List.of("Hemograma"), null);
    private static final VacunaProxima PROXIMA = new VacunaProxima(4L, "Max", "santiago.rojas@correo.com",
            "Polivalente", LocalDate.of(2026, 11, 16), null);

    @Mock private NotificacionRepository notificaciones;
    @Mock private EnviadorNotificaciones enviador;
    private final TransaccionesDePrueba transacciones = new TransaccionesDePrueba();
    private final PlantillasCorreo plantillas = new PlantillasCorreo(RELOJ);
    private NotificarCitaAgendadaUseCase citaAgendada;
    private NotificarCitaCompletadaUseCase citaCompletada;
    private NotificarVacunaProximaUseCase vacunaProxima;

    @BeforeEach
    void prepararCasosDeUso() {
        EnvioSinDuplicados envio = new EnvioSinDuplicados(notificaciones, enviador, transacciones, RELOJ);
        citaAgendada = new NotificarCitaAgendadaUseCase(plantillas, envio);
        citaCompletada = new NotificarCitaCompletadaUseCase(plantillas, envio);
        vacunaProxima = new NotificarVacunaProximaUseCase(plantillas, envio);
    }

    @Test
    void unMensajeNuevoGuardaLaNotificacionYEnviaElCorreoDeUltimoEnLaMismaTransaccion() {
        when(notificaciones.registrarSiEsNueva(any())).thenAnswer(invocacion -> {
            assertThat(transacciones.activa()).isTrue();
            return true;
        });
        doAnswer(invocacion -> {
            assertThat(transacciones.activa()).isTrue();
            return null;
        }).when(enviador).enviar(anyString(), anyString(), anyString());

        assertThat(citaAgendada.ejecutar(MESSAGE_ID, AGENDADA)).isTrue();

        MensajeCorreo esperado = plantillas.citaAgendada(AGENDADA);
        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        InOrder orden = inOrder(notificaciones, enviador);
        orden.verify(notificaciones).registrarSiEsNueva(guardada.capture());
        orden.verify(enviador).enviar("laura.mendez@correo.com", esperado.asunto(), esperado.cuerpo());
        assertThat(guardada.getValue()).isEqualTo(new Notificacion(null, MESSAGE_ID, TipoNotificacion.CITA_AGENDADA,
                "laura.mendez@correo.com", esperado.asunto(), esperado.cuerpo(), AHORA, AHORA));
        assertThat(transacciones.abiertas()).isEqualTo(1);
    }

    @Test
    void unMensajeRepetidoNoVuelveAEnviarElCorreo() {
        when(notificaciones.registrarSiEsNueva(any())).thenReturn(false);

        assertThat(citaAgendada.ejecutar(MESSAGE_ID, AGENDADA)).isFalse();
        assertThat(citaCompletada.ejecutar(MESSAGE_ID, COMPLETADA)).isFalse();
        assertThat(vacunaProxima.ejecutar(MESSAGE_ID, PROXIMA)).isFalse();

        verify(enviador, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void siElEnvioFallaLaExcepcionSaleDeLaTransaccionParaDeshacerlaYReintentar() {
        when(notificaciones.registrarSiEsNueva(any())).thenReturn(true);
        doThrow(new EnvioNoDisponibleException("SMTP caído", null))
                .when(enviador).enviar(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> citaAgendada.ejecutar(MESSAGE_ID, AGENDADA))
                .isInstanceOf(EnvioNoDisponibleException.class);

        assertThat(transacciones.abiertas()).isEqualTo(1);
        assertThat(transacciones.activa()).isFalse();
    }

    @Test
    void elResumenDeLaConsultaVaAlDuenoConSuTipo() {
        when(notificaciones.registrarSiEsNueva(any())).thenReturn(true);

        assertThat(citaCompletada.ejecutar(MESSAGE_ID, COMPLETADA)).isTrue();

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).registrarSiEsNueva(guardada.capture());
        assertThat(guardada.getValue().tipo()).isEqualTo(TipoNotificacion.CITA_COMPLETADA);
        assertThat(guardada.getValue().asunto()).isEqualTo("Resumen de la consulta de Tobby");
        assertThat(guardada.getValue().cuerpo()).contains("Exámenes registrados en la consulta: Hemograma");
        verify(enviador).enviar("laura.mendez@correo.com", "Resumen de la consulta de Tobby",
                guardada.getValue().cuerpo());
    }

    @Test
    void elRecordatorioDeVacunaVaAlDuenoConSuTipo() {
        when(notificaciones.registrarSiEsNueva(any())).thenReturn(true);

        assertThat(vacunaProxima.ejecutar(MESSAGE_ID, PROXIMA)).isTrue();

        ArgumentCaptor<Notificacion> guardada = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).registrarSiEsNueva(guardada.capture());
        assertThat(guardada.getValue().tipo()).isEqualTo(TipoNotificacion.VACUNA_PROXIMA);
        assertThat(guardada.getValue().destino()).isEqualTo("santiago.rojas@correo.com");
        verify(enviador).enviar("santiago.rojas@correo.com", "Recordatorio: vacuna Polivalente de Max",
                guardada.getValue().cuerpo());
    }
}
