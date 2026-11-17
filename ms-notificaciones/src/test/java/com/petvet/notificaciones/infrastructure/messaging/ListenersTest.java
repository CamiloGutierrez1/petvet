package com.petvet.notificaciones.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.petvet.notificaciones.application.NotificarCitaAgendadaUseCase;
import com.petvet.notificaciones.application.NotificarCitaCompletadaUseCase;
import com.petvet.notificaciones.application.NotificarVacunaProximaUseCase;
import com.petvet.notificaciones.application.eventos.CitaAgendada;
import com.petvet.notificaciones.application.eventos.CitaCompletada;
import com.petvet.notificaciones.application.eventos.VacunaProxima;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

@ExtendWith(MockitoExtension.class)
class ListenersTest {

    private static final String AGENDADA = """
            {"citaId": 7, "fechaHora": "2026-11-20T09:30:00", "duenoEmail": "laura.mendez@correo.com", \
            "duenoNombre": "Laura Méndez", "tipoServicio": "VACUNACION", "mascotaNombre": "Tobby", \
            "veterinarioNombre": "Dra. Paula Castaño"}""";
    private static final String COMPLETADA = """
            {"citaId": 7, "duenoEmail": "laura.mendez@correo.com", "diagnostico": "Otitis leve", \
            "mascotaNombre": "Tobby", "proximoControl": null, "proximasVacunas": [], "vacunasAplicadas": []}""";
    private static final String PROXIMA = """
            {"vacuna": "Polivalente", "mascotaId": 4, "duenoEmail": "santiago.rojas@correo.com", \
            "fechaLimite": "2026-11-16", "mascotaNombre": "Max"}""";

    @Mock private NotificarCitaAgendadaUseCase citaAgendada;
    @Mock private NotificarCitaCompletadaUseCase citaCompletada;
    @Mock private NotificarVacunaProximaUseCase vacunaProxima;
    private CitasListener citas;
    private VacunasListener vacunas;

    @BeforeEach
    void crearListeners() {
        citas = new CitasListener(citaAgendada, citaCompletada);
        vacunas = new VacunasListener(vacunaProxima);
    }

    @Test
    void enviaLaCitaAgendadaASuCasoDeUso() {
        citas.recibir(MensajesDePrueba.deCitas("cita.agendada", AGENDADA));

        verify(citaAgendada).ejecutar(MensajesDePrueba.MESSAGE_ID, new CitaAgendada(7L, "Tobby", "Laura Méndez",
                "laura.mendez@correo.com", "Dra. Paula Castaño", LocalDateTime.of(2026, 11, 20, 9, 30), "VACUNACION"));
        verifyNoInteractions(citaCompletada);
    }

    @Test
    void enviaLaCitaCompletadaASuCasoDeUso() {
        citas.recibir(MensajesDePrueba.deCitas("cita.completada", COMPLETADA));

        verify(citaCompletada).ejecutar(MensajesDePrueba.MESSAGE_ID, new CitaCompletada(7L, "Tobby",
                "laura.mendez@correo.com", "Otitis leve", List.of(), null, List.of(), List.of(), null));
        verifyNoInteractions(citaAgendada);
    }

    @Test
    void reprocesaUnMensajeReenviadoDesdeLaDlqPorSuPropiedadType() {
        citas.recibir(MensajesDePrueba.recibido(RabbitTopologia.COLA_CITAS, "cita.completada", COMPLETADA,
                MensajesDePrueba.MESSAGE_ID));

        verify(citaCompletada).ejecutar(eq(MensajesDePrueba.MESSAGE_ID), any(CitaCompletada.class));
    }

    @Test
    void descartaSinErrorLasClavesQueNoGeneranNotificacion() {
        assertThatCode(() -> {
            citas.recibir(MensajesDePrueba.deCitas("cita.cancelada", "{\"citaId\": 7}"));
            citas.recibir(MensajesDePrueba.recibido(RabbitTopologia.COLA_CITAS, null, "{}", null));
            vacunas.recibir(MensajesDePrueba.deCitas("vacuna.aplicada", "no es json"));
        }).doesNotThrowAnyException();

        verifyNoInteractions(citaAgendada, citaCompletada, vacunaProxima);
    }

    @Test
    void rechazaElMensajeSinMessageIdAntesDeLlamarAlCasoDeUso() {
        assertThatThrownBy(() -> citas.recibir(MensajesDePrueba.recibido("cita.agendada", "cita.agendada",
                AGENDADA, null))).isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(citaAgendada, citaCompletada);
    }

    @Test
    void rechazaElJsonInvalidoSinLlamarAlCasoDeUso() {
        assertThatThrownBy(() -> vacunas.recibir(MensajesDePrueba.deCitas("vacuna.proxima", "{\"vacuna\": ")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(vacunaProxima);
    }

    @Test
    void enviaElRecordatorioDeVacunaASuCasoDeUso() {
        vacunas.recibir(MensajesDePrueba.deCitas("vacuna.proxima", PROXIMA));

        verify(vacunaProxima).ejecutar(MensajesDePrueba.MESSAGE_ID, new VacunaProxima(4L, "Max",
                "santiago.rojas@correo.com", "Polivalente", LocalDate.of(2026, 11, 16), null));
    }
}
