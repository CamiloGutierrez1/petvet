package com.petvet.citas.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petvet.citas.domain.PublicadorEventos;
import com.petvet.citas.domain.eventos.CitaAgendadaEvento;
import com.petvet.citas.domain.eventos.CitaCompletadaEvento;
import com.petvet.citas.domain.eventos.VacunaProximaEvento;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** La misma transacción guarda la cita o consulta y el mensaje pendiente de envío. */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxPublicadorEventos implements PublicadorEventos {
    private static final String INSERTAR = """
            INSERT INTO outbox_eventos (id, clave_deduplicacion, routing_key, payload)
            VALUES (?, ?, ?, CAST(? AS jsonb))
            ON CONFLICT (clave_deduplicacion) DO NOTHING
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OutboxPublicadorEventos(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void citaAgendada(CitaAgendadaEvento evento) {
        registrar("cita.agendada", "cita.agendada:" + evento.citaId(), evento);
    }

    @Override
    public void citaCompletada(CitaCompletadaEvento evento) {
        registrar("cita.completada", "cita.completada:" + evento.citaId(), evento);
    }

    @Override
    public void vacunaProxima(VacunaProximaEvento evento) {
        String clave = "vacuna.proxima:" + evento.mascotaId() + ":" + evento.vacuna()
                + ":" + evento.fechaLimite();
        registrar("vacuna.proxima", clave, evento);
    }

    private void registrar(String routingKey, String clave, Object evento) {
        try {
            jdbc.update(INSERTAR, UUID.randomUUID(), clave, routingKey,
                    objectMapper.writeValueAsString(evento));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo serializar el evento de negocio", error);
        }
    }
}
