package com.petvet.citas.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {
    @Mock JdbcTemplate jdbc;
    @Mock RabbitTemplate rabbit;
    @Mock PlatformTransactionManager transactionManager;

    private OutboxDispatcher dispatcher;
    private final OutboxDispatcher.EventoPendiente evento = new OutboxDispatcher.EventoPendiente(
            UUID.fromString("2f02c4a9-80f9-4fd4-9652-cd701a50655e"), "cita.agendada",
            "{\"citaId\":7}", Instant.parse("2026-11-09T12:00:00Z"), 0);

    @BeforeEach
    void preparar() {
        dispatcher = new OutboxDispatcher(jdbc, rabbit, transactionManager);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<OutboxDispatcher.EventoPendiente>>any()))
                .thenReturn(List.of(evento)).thenReturn(List.of());
    }

    @Test
    void confirmaSoloDespuesDelAckYConservaIdentidadYPersistencia() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(eq("petvet.events"), eq("cita.agendada"), any(Message.class), any(CorrelationData.class));

        dispatcher.despachar();

        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("petvet.events"), eq("cita.agendada"), mensaje.capture(), any(CorrelationData.class));
        assertThat(new String(mensaje.getValue().getBody(), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("{\"citaId\":7}");
        assertThat(mensaje.getValue().getMessageProperties().getMessageId()).isEqualTo(evento.id().toString());
        assertThat(mensaje.getValue().getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        verify(jdbc).update(contains("SET enviado_en"), eq(evento.id()));
        verify(jdbc, never()).update(contains("SET intentos"), any(), any(), any());
    }

    @Test
    void mensajeSinColaDeDestinoPermanecePendienteAunqueRabbitConfirme() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            Message message = invocation.getArgument(2);
            correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "petvet.events", "cita.agendada"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        dispatcher.despachar();

        verify(jdbc, never()).update(contains("SET enviado_en"), any(UUID.class));
        verify(jdbc).update(contains("SET intentos"), eq(1), eq(5L), eq(evento.id()));
    }

    @Test
    void rabbitCaidoConservaElEventoParaReintentar() {
        doAnswer(invocation -> {
            throw new AmqpConnectException(new java.net.ConnectException("demo"));
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        dispatcher.despachar();

        verify(jdbc, never()).update(contains("SET enviado_en"), any(UUID.class));
        verify(jdbc).update(contains("SET intentos"), eq(1), eq(5L), eq(evento.id()));
    }

    @Test
    void nackNoSeRegistraComoEntregado() {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "demo nack"));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        dispatcher.despachar();

        verify(jdbc, never()).update(contains("SET enviado_en"), any(UUID.class));
        verify(jdbc).update(contains("SET intentos"), eq(1), eq(5L), eq(evento.id()));
    }

    @Test
    void brokerCaidoDetieneElLoteSinProbarLosDemasEventos() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<OutboxDispatcher.EventoPendiente>>any()))
                .thenReturn(List.of(evento)).thenReturn(List.of(otroEvento())).thenReturn(List.of());
        doAnswer(invocation -> {
            throw new AmqpConnectException(new java.net.ConnectException("demo"));
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        dispatcher.despachar();

        verify(rabbit, times(1)).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        verify(jdbc).update(contains("SET intentos"), eq(1), eq(5L), eq(evento.id()));
        verify(jdbc, never()).update(contains("SET intentos"), any(), any(), eq(otroEvento().id()));
    }

    @Test
    void eventoSinColaNoImpideEnviarElSiguiente() {
        OutboxDispatcher.EventoPendiente otro = otroEvento();
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<OutboxDispatcher.EventoPendiente>>any()))
                .thenReturn(List.of(evento)).thenReturn(List.of(otro)).thenReturn(List.of());
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            Message message = invocation.getArgument(2);
            if ("cita.agendada".equals(invocation.getArgument(1))) {
                correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "petvet.events", "cita.agendada"));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        dispatcher.despachar();

        verify(jdbc).update(contains("SET intentos"), eq(1), eq(5L), eq(evento.id()));
        verify(jdbc).update(contains("SET enviado_en"), eq(otro.id()));
    }

    private OutboxDispatcher.EventoPendiente otroEvento() {
        return new OutboxDispatcher.EventoPendiente(
                UUID.fromString("7b1d2c3e-4f50-4a61-8b72-93a4b5c6d7e8"), "vacuna.proxima",
                "{\"mascotaId\":3}", Instant.parse("2026-11-09T12:00:01Z"), 0);
    }
}
