package com.petvet.citas.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reenvía con el mismo messageId hasta recibir confirmación y una ruta válida.
 * Un cierre entre el ack y el commit puede repetir el mensaje en RabbitMQ.
 */
@Component
@ConditionalOnProperty(name = "petvet.eventos.habilitados", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final int LOTE_MAXIMO = 50;
    private static final String SIGUIENTE = """
            SELECT id, routing_key, payload::text, creado_en, intentos
            FROM outbox_eventos
            WHERE enviado_en IS NULL AND proximo_intento <= CURRENT_TIMESTAMP
            ORDER BY creado_en, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """;

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate transactions;

    public OutboxDispatcher(JdbcTemplate jdbc, RabbitTemplate rabbit,
            PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${petvet.eventos.intervalo-ms:5000}")
    public void despachar() {
        for (int i = 0; i < LOTE_MAXIMO; i++) {
            Boolean continuar = transactions.execute(status -> procesarSiguiente());
            if (!Boolean.TRUE.equals(continuar) || Thread.currentThread().isInterrupted()) {
                return;
            }
        }
    }

    /** Devuelve verdadero si conviene seguir con el siguiente evento pendiente en esta ejecución. */
    private boolean procesarSiguiente() {
        List<EventoPendiente> pendientes = jdbc.query(SIGUIENTE, (rs, rowNum) ->
                new EventoPendiente(rs.getObject("id", UUID.class), rs.getString("routing_key"),
                        rs.getString("payload"), rs.getTimestamp("creado_en").toInstant(),
                        rs.getInt("intentos")));
        if (pendientes.isEmpty()) {
            return false;
        }
        EventoPendiente evento = pendientes.getFirst();
        try {
            CorrelationData correlation = new CorrelationData(evento.id().toString());
            rabbit.send(RabbitConfiguration.EXCHANGE, evento.routingKey(), mensaje(evento), correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(3, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                // El broker respondió. Otro evento puede tener una cola enlazada, así que se continúa.
                reintentar(evento, "sin confirmación o sin cola de destino");
                return true;
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            reintentar(evento, "interrupción del envío");
            return false;
        } catch (Exception error) {
            // Broker caído o sin respuesta. Los demás eventos fallarían igual tras su tiempo de espera.
            reintentar(evento, error.getClass().getSimpleName());
            return false;
        }
        jdbc.update("UPDATE outbox_eventos SET enviado_en = CURRENT_TIMESTAMP WHERE id = ?", evento.id());
        return true;
    }

    private Message mensaje(EventoPendiente evento) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(evento.id().toString());
        properties.setTimestamp(Date.from(evento.creadoEn()));
        properties.setType(evento.routingKey());
        return new Message(evento.payload().getBytes(StandardCharsets.UTF_8), properties);
    }

    private void reintentar(EventoPendiente evento, String motivo) {
        int intentos = Math.min(evento.intentos() + 1, 1_000_000);
        long demoraSegundos = Math.min(300L, 5L * intentos);
        jdbc.update("""
                UPDATE outbox_eventos
                SET intentos = ?, proximo_intento = CURRENT_TIMESTAMP + (? * INTERVAL '1 second')
                WHERE id = ?
                """, intentos, demoraSegundos, evento.id());
        LOG.warn("Evento {} ({}) pendiente de reintento, intento {}. {}",
                evento.id(), evento.routingKey(), intentos, motivo);
    }

    record EventoPendiente(UUID id, String routingKey, String payload, Instant creadoEn, int intentos) {
    }
}
