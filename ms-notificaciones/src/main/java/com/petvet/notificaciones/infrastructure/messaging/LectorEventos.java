package com.petvet.notificaciones.infrastructure.messaging;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.Set;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * Lee los mensajes de Citas tal como los arma su outbox: JSON en UTF-8, sin __TypeId__, con el UUID
 * de la outbox en messageId y la routing key repetida en type. Lo que no se puede leer se rechaza sin
 * reencolar, así que termina en la DLQ de la cola.
 */
final class LectorEventos {
    private static final int LARGO_MAXIMO_MESSAGE_ID = 255;
    private static final Set<String> CLAVES = Set.of(
            RabbitTopologia.CITA_AGENDADA, RabbitTopologia.CITA_COMPLETADA, RabbitTopologia.VACUNA_PROXIMA);

    // Configuración explícita para no depender de los valores por defecto de Spring Boot: fechas ISO
    // y campos desconocidos ignorados, como examenes o los que Citas agregue después.
    private static final ObjectMapper JSON = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private LectorEventos() {
    }

    /**
     * La routing key con que llegó el mensaje. Al reenviarlo desde una DLQ se pierde la clave
     * original, así que se usa la propiedad type que Citas llena con la misma clave.
     */
    static String clave(Message mensaje) {
        MessageProperties propiedades = mensaje.getMessageProperties();
        String recibida = propiedades.getReceivedRoutingKey();
        return recibida != null && CLAVES.contains(recibida) ? recibida : propiedades.getType();
    }

    static String messageId(Message mensaje) {
        String messageId = mensaje.getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            throw new AmqpRejectAndDontRequeueException("El mensaje no trae messageId y no se puede deduplicar");
        }
        if (messageId.length() > LARGO_MAXIMO_MESSAGE_ID) {
            throw new AmqpRejectAndDontRequeueException("El messageId supera " + LARGO_MAXIMO_MESSAGE_ID + " caracteres");
        }
        return messageId;
    }

    static <T> T leer(Message mensaje, Class<T> tipo) {
        try {
            T evento = JSON.readValue(mensaje.getBody(), tipo);
            if (evento == null) {
                throw new AmqpRejectAndDontRequeueException("El mensaje no trae un cuerpo JSON");
            }
            return evento;
        } catch (IOException error) {
            throw new AmqpRejectAndDontRequeueException("El cuerpo no corresponde a " + tipo.getSimpleName(), error);
        }
    }
}
