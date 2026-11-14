package com.petvet.notificaciones.infrastructure.messaging;

import java.util.Map;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;

/**
 * Los reintentos del listener (spring.rabbitmq.listener.simple.retry) cubren fallas pasajeras, como
 * el SMTP o la base caídos; al agotarse, el mensaje va a la DLQ. Spring Boot reintenta cualquier
 * excepción, pero un mensaje sin messageId o con JSON inválido falla igual cada vez, así que esos
 * van directo a la DLQ.
 */
@Configuration
public class PoliticaReintentos {

    @Bean
    public RabbitRetryTemplateCustomizer sinReintentosParaMensajesInvalidos(RabbitProperties propiedades) {
        int intentos = propiedades.getListener().getSimple().getRetry().getMaxAttempts();
        return (destino, plantilla) -> {
            if (destino == RabbitRetryTemplateCustomizer.Target.LISTENER) {
                plantilla.setRetryPolicy(politica(intentos));
            }
        };
    }

    /** Revisa toda la cadena de causas, porque el contenedor envuelve la excepción del listener. */
    static SimpleRetryPolicy politica(int intentos) {
        return new SimpleRetryPolicy(intentos, Map.of(AmqpRejectAndDontRequeueException.class, false), true, true);
    }
}
