package com.petvet.notificaciones.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petvet.notificaciones.application.eventos.CitaAgendada;
import com.petvet.notificaciones.application.eventos.CitaCompletada;
import com.petvet.notificaciones.application.eventos.VacunaPendiente;
import com.petvet.notificaciones.application.eventos.VacunaProxima;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

/**
 * Contrato con ms-citas-historial. Los cuerpos son el texto exacto de la columna jsonb de su outbox
 * (payload::text): jsonb ordena las claves por largo y separa con ", " y ": ".
 */
class ContratoEventosCitasTest {

    private static final String CITA_AGENDADA = """
            {"citaId": 7, "fechaHora": "2026-11-20T09:30:00", "duenoEmail": "laura.mendez@correo.com", \
            "duenoNombre": "Laura Méndez", "tipoServicio": "VACUNACION", "mascotaNombre": "Tobby", \
            "veterinarioNombre": "Dra. Paula Castaño"}""";

    private static final String CITA_COMPLETADA = """
            {"citaId": 7, "duenoEmail": "laura.mendez@correo.com", "diagnostico": "Otitis leve", \
            "mascotaNombre": "Tobby", "proximoControl": "2026-12-04", "proximasVacunas": [{"vacuna": \
            "Polivalente", "fechaLimite": "2026-12-13"}, {"vacuna": "Rabia", "fechaLimite": "2027-11-20"}], \
            "vacunasAplicadas": ["Rabia"]}""";

    /** El mismo evento cuando Citas agregue los exámenes de la consulta (campo opcional). */
    private static final String CITA_COMPLETADA_CON_EXAMENES = """
            {"citaId": 7, "examenes": ["Hemograma", "Otoscopia"], "duenoEmail": "laura.mendez@correo.com", \
            "diagnostico": "Otitis leve", "mascotaNombre": "Tobby", "proximoControl": "2026-12-04", \
            "proximasVacunas": [{"vacuna": "Polivalente", "fechaLimite": "2026-12-13"}, {"vacuna": "Rabia", \
            "fechaLimite": "2027-11-20"}], "vacunasAplicadas": ["Rabia"]}""";

    private static final String VACUNA_PROXIMA = """
            {"vacuna": "Polivalente", "mascotaId": 4, "duenoEmail": "santiago.rojas@correo.com", \
            "fechaLimite": "2026-11-16", "mascotaNombre": "Max"}""";

    private static final List<VacunaPendiente> PROXIMAS = List.of(
            new VacunaPendiente("Polivalente", LocalDate.of(2026, 12, 13)),
            new VacunaPendiente("Rabia", LocalDate.of(2027, 11, 20)));

    @Test
    void leeLaCitaAgendadaTalComoLaPublicaCitas() {
        CitaAgendada evento = LectorEventos.leer(MensajesDePrueba.deCitas("cita.agendada", CITA_AGENDADA),
                CitaAgendada.class);

        assertThat(evento).isEqualTo(new CitaAgendada(7L, "Tobby", "Laura Méndez", "laura.mendez@correo.com",
                "Dra. Paula Castaño", LocalDateTime.of(2026, 11, 20, 9, 30), "VACUNACION"));
    }

    @Test
    void leeLaCitaCompletadaSinExamenes() {
        CitaCompletada evento = LectorEventos.leer(MensajesDePrueba.deCitas("cita.completada", CITA_COMPLETADA),
                CitaCompletada.class);

        assertThat(evento).isEqualTo(new CitaCompletada(7L, "Tobby", "laura.mendez@correo.com", "Otitis leve",
                List.of("Rabia"), LocalDate.of(2026, 12, 4), PROXIMAS, List.of(), null));
    }

    @Test
    void leeLosExamenesCuandoCitasLosPublica() {
        CitaCompletada evento = LectorEventos.leer(
                MensajesDePrueba.deCitas("cita.completada", CITA_COMPLETADA_CON_EXAMENES), CitaCompletada.class);

        assertThat(evento.examenes()).containsExactly("Hemograma", "Otoscopia");
        assertThat(evento.proximasVacunas()).isEqualTo(PROXIMAS);
    }

    @Test
    void aceptaControlNuloListasVaciasYCamposQueNoConoce() {
        String cuerpo = """
                {"citaId": 12, "version": 2, "cirugias": [], "duenoEmail": "santiago.rojas@correo.com", \
                "diagnostico": "Sin hallazgos", "mascotaNombre": "Kiwi", "proximoControl": null, \
                "proximasVacunas": [], "vacunasAplicadas": []}""";

        CitaCompletada evento = LectorEventos.leer(MensajesDePrueba.deCitas("cita.completada", cuerpo),
                CitaCompletada.class);

        assertThat(evento).isEqualTo(new CitaCompletada(12L, "Kiwi", "santiago.rojas@correo.com", "Sin hallazgos",
                List.of(), null, List.of(), List.of(), null));
    }

    @Test
    void leeElRecordatorioDeVacuna() {
        VacunaProxima evento = LectorEventos.leer(MensajesDePrueba.deCitas("vacuna.proxima", VACUNA_PROXIMA),
                VacunaProxima.class);

        assertThat(evento).isEqualTo(new VacunaProxima(4L, "Max", "santiago.rojas@correo.com", "Polivalente",
                LocalDate.of(2026, 11, 16), null));
    }

    @Test
    void leeElNombreDelDuenoSiCitasLoAgregaAlRecordatorio() {
        String cuerpo = """
                {"vacuna": "Rabia", "mascotaId": 4, "duenoEmail": "santiago.rojas@correo.com", \
                "duenoNombre": "Santiago Rojas", "fechaLimite": "2026-11-16", "mascotaNombre": "Max"}""";

        VacunaProxima evento = LectorEventos.leer(MensajesDePrueba.deCitas("vacuna.proxima", cuerpo),
                VacunaProxima.class);

        assertThat(evento.duenoNombre()).isEqualTo("Santiago Rojas");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"citaId\": 7, \"fechaHora\": \"2026-", "no es json", "null", ""})
    void rechazaSinReencolarUnCuerpoQueNoSePuedeLeer(String cuerpo) {
        Message mensaje = MensajesDePrueba.deCitas("cita.agendada", cuerpo);

        assertThatThrownBy(() -> LectorEventos.leer(mensaje, CitaAgendada.class))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void rechazaSinReencolarUnEventoAlQueLeFaltaUnCampoObligatorio() {
        String sinCorreo = """
                {"citaId": 7, "fechaHora": "2026-11-20T09:30:00", "tipoServicio": "VACUNACION", \
                "mascotaNombre": "Tobby", "veterinarioNombre": "Dra. Paula Castaño"}""";
        Message mensaje = MensajesDePrueba.deCitas("cita.agendada", sinCorreo);

        assertThatThrownBy(() -> LectorEventos.leer(mensaje, CitaAgendada.class))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("El cuerpo no corresponde a CitaAgendada")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("El evento no trae el campo duenoEmail");
    }

    @Test
    void rechazaUnaVacunaPendienteIncompleta() {
        String cuerpo = """
                {"citaId": 7, "duenoEmail": "laura.mendez@correo.com", "diagnostico": "Sano", \
                "mascotaNombre": "Tobby", "proximoControl": null, "proximasVacunas": [{"vacuna": "Rabia"}], \
                "vacunasAplicadas": []}""";
        Message mensaje = MensajesDePrueba.deCitas("cita.completada", cuerpo);

        assertThatThrownBy(() -> LectorEventos.leer(mensaje, CitaCompletada.class))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasRootCauseMessage("El evento no trae el campo proximasVacunas.fechaLimite");
    }

    @Test
    void elMessageIdEsLaClaveDeDeduplicacionYPuedeNoSerUnUuid() {
        assertThat(LectorEventos.messageId(MensajesDePrueba.deCitas("cita.agendada", CITA_AGENDADA)))
                .isEqualTo(MensajesDePrueba.MESSAGE_ID);
        assertThat(LectorEventos.messageId(MensajesDePrueba.recibido("cita.agendada", "cita.agendada",
                CITA_AGENDADA, "prueba-manual-1"))).isEqualTo("prueba-manual-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rechazaSinReencolarUnMensajeSinMessageId(String messageId) {
        Message vacio = MensajesDePrueba.recibido("cita.agendada", "cita.agendada", CITA_AGENDADA, messageId);
        Message nulo = MensajesDePrueba.recibido("cita.agendada", "cita.agendada", CITA_AGENDADA, null);

        assertThatThrownBy(() -> LectorEventos.messageId(vacio))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> LectorEventos.messageId(nulo))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("El mensaje no trae messageId y no se puede deduplicar");
    }

    @Test
    void rechazaUnMessageIdQueNoCabeEnLaTabla() {
        Message largo = MensajesDePrueba.recibido("cita.agendada", "cita.agendada", CITA_AGENDADA, "x".repeat(256));

        assertThatThrownBy(() -> LectorEventos.messageId(largo))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThat(LectorEventos.messageId(MensajesDePrueba.recibido("cita.agendada", "cita.agendada",
                CITA_AGENDADA, "x".repeat(255)))).hasSize(255);
    }

    @Test
    void tomaLaClaveDeLaRoutingKeyYSiNoLaReconoceUsaType() {
        assertThat(LectorEventos.clave(MensajesDePrueba.deCitas("cita.completada", CITA_COMPLETADA)))
                .isEqualTo("cita.completada");
        // Reenviado desde la DLQ por el exchange por defecto: la routing key es el nombre de la cola.
        assertThat(LectorEventos.clave(MensajesDePrueba.recibido(RabbitTopologia.COLA_CITAS, "cita.completada",
                CITA_COMPLETADA, MensajesDePrueba.MESSAGE_ID))).isEqualTo("cita.completada");
        assertThat(LectorEventos.clave(MensajesDePrueba.recibido("cita.cancelada", null, "{}",
                MensajesDePrueba.MESSAGE_ID))).isNull();
    }
}
