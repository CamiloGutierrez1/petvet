package com.petvet.notificaciones.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.petvet.notificaciones.application.eventos.CitaAgendada;
import com.petvet.notificaciones.application.eventos.CitaCompletada;
import com.petvet.notificaciones.application.eventos.VacunaPendiente;
import com.petvet.notificaciones.application.eventos.VacunaProxima;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlantillasCorreoTest {

    /** Viernes 13 de noviembre de 2026 al mediodía en Bogotá. */
    private static final Clock HOY = Clock.fixed(Instant.parse("2026-11-13T17:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime FECHA_CITA = LocalDateTime.of(2026, 11, 20, 9, 30);

    private final PlantillasCorreo plantillas = new PlantillasCorreo(HOY);

    @Test
    void confirmaLaCitaConFechaEnEspanolHoraDe24HorasVeterinarioYServicio() {
        MensajeCorreo correo = plantillas.citaAgendada(new CitaAgendada(7L, "Tobby", "Laura Méndez",
                "laura.mendez@correo.com", "Dra. Paula Castaño", FECHA_CITA, "VACUNACION"));

        assertThat(correo.asunto()).isEqualTo("Confirmación de la cita de Tobby");
        assertThat(correo.cuerpo()).isEqualTo("""
                Hola, Laura Méndez:

                La cita de vacunación de Tobby con la Dra. Paula Castaño quedó para el viernes 20 de noviembre de 2026 a las 09:30.

                Si no puede asistir, cancele la cita en PetVet para liberar el turno.

                Equipo PetVet
                """);
    }

    @ParameterizedTest
    @CsvSource({
            "CONSULTA_GENERAL, La cita de consulta general de Misha",
            "VACUNACION, La cita de vacunación de Misha",
            "CONTROL, La cita de control de Misha",
            "ESPECIALISTA, La cita de consulta con especialista de Misha",
            "CIRUGIA_MENOR, La cita de cirugia menor de Misha"})
    void escribeElServicioConNombreLegible(String tipoServicio, String esperado) {
        MensajeCorreo correo = plantillas.citaAgendada(new CitaAgendada(8L, "Misha", "Laura Méndez",
                "laura.mendez@correo.com", "Dra. Paula Castaño", FECHA_CITA, tipoServicio));

        assertThat(correo.cuerpo()).contains(esperado + " con la Dra. Paula Castaño");
        assertThat(correo.cuerpo()).doesNotContain(tipoServicio);
    }

    @Test
    void usaElArticuloDelVeterinarioYUnaHoraDeTardeSinAmPm() {
        MensajeCorreo doctor = plantillas.citaAgendada(new CitaAgendada(9L, "Kiwi", "Santiago Rojas",
                "santiago.rojas@correo.com", "Dr. Andrés Villamil", LocalDateTime.of(2026, 11, 17, 15, 0), "CONTROL"));
        MensajeCorreo sinTitulo = plantillas.citaAgendada(new CitaAgendada(10L, "Kiwi", "Santiago Rojas",
                "santiago.rojas@correo.com", "Valentina Ríos", FECHA_CITA, "CONTROL"));

        assertThat(doctor.cuerpo()).contains("con el Dr. Andrés Villamil quedó para el martes 17 de noviembre de 2026 a las 15:00.");
        assertThat(doctor.cuerpo()).doesNotContain("p. m.", "a. m.", " ");
        assertThat(sinTitulo.cuerpo()).contains("con Valentina Ríos quedó");
    }

    @Test
    void saludaSinNombreCuandoElEventoNoLoTrae() {
        MensajeCorreo correo = plantillas.citaAgendada(new CitaAgendada(11L, "Tobby", " ",
                "laura.mendez@correo.com", "Dra. Paula Castaño", FECHA_CITA, "CONTROL"));

        assertThat(correo.cuerpo()).startsWith("Hola:\n\nLa cita de control de Tobby");
    }

    @Test
    void resumeLaConsultaConExamenesProximoControlYProximasVacunas() {
        MensajeCorreo correo = plantillas.citaCompletada(new CitaCompletada(7L, "Tobby", "laura.mendez@correo.com",
                "Otitis leve", List.of("Rabia"), LocalDate.of(2026, 12, 4),
                List.of(new VacunaPendiente("Polivalente", LocalDate.of(2026, 12, 13)),
                        new VacunaPendiente("Rabia", LocalDate.of(2027, 11, 20))),
                List.of("Hemograma", "Otoscopia"), null));

        assertThat(correo.asunto()).isEqualTo("Resumen de la consulta de Tobby");
        assertThat(correo.cuerpo()).isEqualTo("""
                Hola:

                Este es el resumen de la consulta de Tobby.

                Diagnóstico: Otitis leve
                Vacunas aplicadas: Rabia
                Exámenes registrados en la consulta: Hemograma, Otoscopia
                Próximo control: viernes 4 de diciembre de 2026

                Próximas vacunas:
                - Polivalente: antes del domingo 13 de diciembre de 2026
                - Rabia: antes del sábado 20 de noviembre de 2027

                Equipo PetVet
                """);
    }

    @Test
    void sinExamenesNiControlNiVacunasLoDiceSinInventarDatos() {
        MensajeCorreo correo = plantillas.citaCompletada(new CitaCompletada(12L, "Kiwi", "santiago.rojas@correo.com",
                "Revisión de plumaje sin hallazgos", null, null, null, null, null));

        assertThat(correo.cuerpo())
                .contains("Vacunas aplicadas: ninguna\n")
                .contains("Próximo control: sin control programado\n")
                .contains("Próximas vacunas: ninguna pendiente\n")
                .doesNotContain("Exámenes", "resultado");
    }

    @Test
    void marcaLasVacunasVencidasYLasQueVencenHoy() {
        MensajeCorreo correo = plantillas.citaCompletada(new CitaCompletada(13L, "Misha", "laura.mendez@correo.com",
                "Control anual", List.of(), null,
                List.of(new VacunaPendiente("Rabia", LocalDate.of(2026, 10, 2)),
                        new VacunaPendiente("Triple felina", LocalDate.of(2026, 11, 13))),
                List.of(), null));

        assertThat(correo.cuerpo())
                .contains("- Rabia: venció el viernes 2 de octubre de 2026\n")
                .contains("- Triple felina: vence hoy, viernes 13 de noviembre de 2026\n");
    }

    @Test
    void saludaPorNombreSiCitasLlegaAPublicarloEnElResumen() {
        MensajeCorreo correo = plantillas.citaCompletada(new CitaCompletada(14L, "Tobby", "laura.mendez@correo.com",
                "Sano", List.of(), null, List.of(), List.of(), "Laura Méndez"));

        assertThat(correo.cuerpo()).startsWith("Hola, Laura Méndez:\n");
    }

    @Test
    void recuerdaLaVacunaConSuFechaLimite() {
        MensajeCorreo correo = plantillas.vacunaProxima(new VacunaProxima(4L, "Max", "santiago.rojas@correo.com",
                "Polivalente", LocalDate.of(2026, 11, 16), null));

        assertThat(correo.asunto()).isEqualTo("Recordatorio: vacuna Polivalente de Max");
        assertThat(correo.cuerpo()).isEqualTo("""
                Hola:

                La vacuna Polivalente de su mascota Max va a vencer pronto. Aplíquela antes del lunes 16 de noviembre de 2026.

                Puede agendar la cita de vacunación en PetVet.

                Equipo PetVet
                """);
    }

    @Test
    void elRecordatorioCambiaSiLaVacunaVenceHoyOYaVencio() {
        MensajeCorreo hoy = plantillas.vacunaProxima(new VacunaProxima(4L, "Max", "santiago.rojas@correo.com",
                "Rabia", LocalDate.of(2026, 11, 13), "Santiago Rojas"));
        MensajeCorreo vencida = plantillas.vacunaProxima(new VacunaProxima(4L, "Max", "santiago.rojas@correo.com",
                "Rabia", LocalDate.of(2026, 10, 2), null));

        assertThat(hoy.cuerpo()).startsWith("Hola, Santiago Rojas:\n")
                .contains("La vacuna Rabia de su mascota Max vence hoy, viernes 13 de noviembre de 2026.");
        assertThat(vencida.cuerpo()).contains("La vacuna Rabia de su mascota Max venció el viernes 2 de octubre de 2026.")
                .doesNotContain("va a vencer pronto");
    }
}
