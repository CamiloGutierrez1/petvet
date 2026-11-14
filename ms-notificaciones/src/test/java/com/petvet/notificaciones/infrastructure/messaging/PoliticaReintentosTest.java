package com.petvet.notificaciones.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petvet.notificaciones.domain.EnvioNoDisponibleException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.mail.MailSendException;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

class PoliticaReintentosTest {

    private static final Message MENSAJE = new Message(new byte[0], new MessageProperties());

    @Test
    void unMensajeInvalidoVaDirectoALaDlqSinReintentos() {
        RetryTemplate plantilla = plantillaDelListener();
        AtomicInteger intentos = new AtomicInteger();

        // El contenedor envuelve la excepción del listener; la política revisa la causa.
        assertThatThrownBy(() -> plantilla.execute(contexto -> {
            intentos.incrementAndGet();
            throw new ListenerExecutionFailedException("Falló el listener",
                    new AmqpRejectAndDontRequeueException("El cuerpo no corresponde a CitaAgendada"), MENSAJE);
        })).isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(intentos).hasValue(1);
    }

    @Test
    void unaFallaDelSmtpSeReintentaHastaAgotarLosIntentos() {
        RetryTemplate plantilla = plantillaDelListener();
        AtomicInteger intentos = new AtomicInteger();

        assertThatThrownBy(() -> plantilla.execute(contexto -> {
            intentos.incrementAndGet();
            throw new ListenerExecutionFailedException("Falló el listener", new EnvioNoDisponibleException(
                    "No fue posible enviar el correo por SMTP", new MailSendException("Connection refused")), MENSAJE);
        })).isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(intentos).hasValue(3);
    }

    @Test
    void noCambiaLosReintentosAlPublicar() {
        RetryTemplate plantilla = new RetryTemplate();
        SimpleRetryPolicy original = new SimpleRetryPolicy(5);
        plantilla.setRetryPolicy(original);

        new PoliticaReintentos().sinReintentosParaMensajesInvalidos(new RabbitProperties())
                .customize(RabbitRetryTemplateCustomizer.Target.SENDER, plantilla);

        AtomicInteger intentos = new AtomicInteger();
        plantilla.setBackOffPolicy(new NoBackOffPolicy());
        assertThatThrownBy(() -> plantilla.execute(contexto -> {
            intentos.incrementAndGet();
            throw new AmqpRejectAndDontRequeueException("igual se reintenta");
        })).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThat(intentos).hasValue(5);
    }

    /** Lo que hace Spring Boot con spring.rabbitmq.listener.simple.retry.max-attempts=3, sin esperas. */
    private static RetryTemplate plantillaDelListener() {
        RabbitProperties propiedades = new RabbitProperties();
        propiedades.getListener().getSimple().getRetry().setMaxAttempts(3);
        RetryTemplate plantilla = new RetryTemplate();
        new PoliticaReintentos().sinReintentosParaMensajesInvalidos(propiedades)
                .customize(RabbitRetryTemplateCustomizer.Target.LISTENER, plantilla);
        plantilla.setBackOffPolicy(new NoBackOffPolicy());
        return plantilla;
    }
}
