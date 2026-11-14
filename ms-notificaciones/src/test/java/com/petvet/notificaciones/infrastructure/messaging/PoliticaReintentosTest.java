package com.petvet.notificaciones.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petvet.notificaciones.domain.DestinoInvalidoException;
import com.petvet.notificaciones.domain.EnvioNoDisponibleException;
import jakarta.mail.internet.AddressException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailSendException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

class PoliticaReintentosTest {

    private static final Message MENSAJE = new Message(new byte[0], new MessageProperties());

    /** spring.rabbitmq tal como queda con application.yml. */
    private final RabbitProperties propiedades = propiedadesDeLaAplicacion();
    /** Esperas entre intentos, en milisegundos. Se registran sin dormir. */
    private final List<Long> esperas = new ArrayList<>();

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
        assertThat(esperas).isEmpty();
    }

    @Test
    void unaDireccionDeDestinoInvalidaVaDirectoALaDlqSinReintentos() {
        RetryTemplate plantilla = plantillaDelListener();
        AtomicInteger intentos = new AtomicInteger();

        assertThatThrownBy(() -> plantilla.execute(contexto -> {
            intentos.incrementAndGet();
            throw new ListenerExecutionFailedException("Falló el listener", new DestinoInvalidoException(
                    "La dirección de destino no es válida",
                    new AddressException("Illegal address", "laura,mendez@correo.com")), MENSAJE);
        })).isInstanceOf(ListenerExecutionFailedException.class);

        assertThat(intentos).hasValue(1);
        assertThat(esperas).isEmpty();
    }

    @Test
    void unaFallaDelSmtpSeReintentaUnos15SegundosAntesDeIrALaDlq() {
        RetryTemplate plantilla = plantillaDelListener();
        AtomicInteger intentos = new AtomicInteger();

        assertThatThrownBy(() -> plantilla.execute(contexto -> {
            intentos.incrementAndGet();
            throw new ListenerExecutionFailedException("Falló el listener", new EnvioNoDisponibleException(
                    "No fue posible enviar el correo por SMTP", new MailSendException("Connection refused")), MENSAJE);
        })).isInstanceOf(ListenerExecutionFailedException.class);

        // Un reinicio de MailHog de hasta unos 15 s no deja el correo en la DLQ.
        assertThat(propiedades.getListener().getSimple().getRetry().isEnabled()).isTrue();
        assertThat(intentos).hasValue(5);
        assertThat(esperas).containsExactly(1_000L, 2_000L, 4_000L, 8_000L);
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

    /**
     * Lo que arma Spring Boot para el listener con spring.rabbitmq.listener.simple.retry: intentos de
     * max-attempts y esperas exponenciales con initial-interval, multiplier y max-interval. Después
     * aplica el customizer de PoliticaReintentos.
     */
    private RetryTemplate plantillaDelListener() {
        RabbitProperties.ListenerRetry reintentos = propiedades.getListener().getSimple().getRetry();
        ExponentialBackOffPolicy espera = new ExponentialBackOffPolicy();
        espera.setInitialInterval(reintentos.getInitialInterval().toMillis());
        espera.setMultiplier(reintentos.getMultiplier());
        espera.setMaxInterval(reintentos.getMaxInterval().toMillis());
        espera.setSleeper(esperas::add);
        RetryTemplate plantilla = new RetryTemplate();
        plantilla.setRetryPolicy(new SimpleRetryPolicy(reintentos.getMaxAttempts()));
        plantilla.setBackOffPolicy(espera);
        new PoliticaReintentos().sinReintentosParaMensajesInvalidos(propiedades)
                .customize(RabbitRetryTemplateCustomizer.Target.LISTENER, plantilla);
        return plantilla;
    }

    private static RabbitProperties propiedadesDeLaAplicacion() {
        try {
            var fuentes = new YamlPropertySourceLoader().load("application.yml",
                    new ClassPathResource("application.yml"));
            return new Binder(ConfigurationPropertySources.from(fuentes))
                    .bind("spring.rabbitmq", RabbitProperties.class).get();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
