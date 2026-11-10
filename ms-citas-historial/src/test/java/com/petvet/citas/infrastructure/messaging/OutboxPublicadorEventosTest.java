package com.petvet.citas.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petvet.citas.domain.eventos.CitaAgendadaEvento;
import com.petvet.citas.domain.eventos.VacunaProximaEvento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxPublicadorEventosTest {
    @Mock JdbcTemplate jdbc;

    @Test
    void conservaElContratoJsonDeCitaAgendada() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OutboxPublicadorEventos publicador = new OutboxPublicadorEventos(jdbc, mapper);
        CitaAgendadaEvento evento = new CitaAgendadaEvento(7, "Mascota demo", "Dueño demo",
                "demo@example.test", "Vet demo", LocalDateTime.of(2026, 11, 20, 9, 30), "VACUNACION");

        publicador.citaAgendada(evento);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(anyString(), any(UUID.class), eq("cita.agendada:7"), eq("cita.agendada"), json.capture());
        assertThat(mapper.readTree(json.getValue()).get("citaId").asLong()).isEqualTo(7);
        assertThat(mapper.readTree(json.getValue()).get("duenoEmail").asText()).isEqualTo("demo@example.test");
        assertThat(mapper.readTree(json.getValue()).get("fechaHora").asText()).isEqualTo("2026-11-20T09:30:00");
    }

    @Test
    void laClaveDeRecordatorioEsEstableEntreEjecuciones() {
        OutboxPublicadorEventos publicador = new OutboxPublicadorEventos(jdbc, new ObjectMapper().findAndRegisterModules());
        VacunaProximaEvento evento = new VacunaProximaEvento(3, "Mascota demo", "demo@example.test",
                "Vacuna demo", LocalDate.of(2026, 11, 16));

        publicador.vacunaProxima(evento);

        verify(jdbc).update(anyString(), any(UUID.class), eq("vacuna.proxima:3:Vacuna demo:2026-11-16"),
                eq("vacuna.proxima"), anyString());
    }
}
