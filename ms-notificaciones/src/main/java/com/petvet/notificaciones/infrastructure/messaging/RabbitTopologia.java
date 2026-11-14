package com.petvet.notificaciones.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Notificaciones es dueño de sus colas y las declara al conectarse, junto con el exchange de Citas,
 * para no depender del orden de arranque. Los argumentos quedan fijos: si se cambian sobre colas ya
 * creadas, RabbitMQ rechaza la declaración con PRECONDITION_FAILED.
 */
@Configuration
public class RabbitTopologia {
    /** Exchange que publica Citas. Se declara igual que allá: topic, durable y sin autoDelete. */
    public static final String EXCHANGE = "petvet.events";
    public static final String COLA_CITAS = "notificaciones.citas";
    public static final String COLA_VACUNAS = "notificaciones.vacunas";
    /** Exchange propio que recibe los mensajes rechazados y los deja en la DLQ de su cola. */
    public static final String EXCHANGE_DLQ = "petvet.notificaciones.dlx";
    public static final String DLQ_CITAS = COLA_CITAS + ".dlq";
    public static final String DLQ_VACUNAS = COLA_VACUNAS + ".dlq";

    static final String CITA_AGENDADA = "cita.agendada";
    static final String CITA_COMPLETADA = "cita.completada";
    static final String VACUNA_PROXIMA = "vacuna.proxima";

    @Bean
    public TopicExchange petvetEvents() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange exchangeMensajesFallidos() {
        return new DirectExchange(EXCHANGE_DLQ, true, false);
    }

    @Bean
    public Queue colaCitas() {
        return colaConDlq(COLA_CITAS, DLQ_CITAS);
    }

    @Bean
    public Queue colaVacunas() {
        return colaConDlq(COLA_VACUNAS, DLQ_VACUNAS);
    }

    @Bean
    public Queue dlqCitas() {
        return QueueBuilder.durable(DLQ_CITAS).build();
    }

    @Bean
    public Queue dlqVacunas() {
        return QueueBuilder.durable(DLQ_VACUNAS).build();
    }

    @Bean
    public Binding citasDesdeEventos() {
        return BindingBuilder.bind(colaCitas()).to(petvetEvents()).with("cita.*");
    }

    @Bean
    public Binding vacunasDesdeEventos() {
        return BindingBuilder.bind(colaVacunas()).to(petvetEvents()).with("vacuna.*");
    }

    @Bean
    public Binding dlqCitasDesdeFallidos() {
        return BindingBuilder.bind(dlqCitas()).to(exchangeMensajesFallidos()).with(DLQ_CITAS);
    }

    @Bean
    public Binding dlqVacunasDesdeFallidos() {
        return BindingBuilder.bind(dlqVacunas()).to(exchangeMensajesFallidos()).with(DLQ_VACUNAS);
    }

    private static Queue colaConDlq(String nombre, String dlq) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(EXCHANGE_DLQ)
                .deadLetterRoutingKey(dlq)
                .build();
    }
}
