package com.petvet.notificaciones.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;

/** Arma los mensajes con las mismas propiedades que pone el OutboxDispatcher de Citas. */
final class MensajesDePrueba {
    static final String MESSAGE_ID = "6b1f3c2e-9d7a-4f0e-8c55-0a4b2d9e7f13";

    private MensajesDePrueba() {
    }

    static Message deCitas(String clave, String cuerpo) {
        return recibido(clave, clave, cuerpo, MESSAGE_ID);
    }

    /**
     * @param recibidaCon routing key con la que el mensaje llegó a la cola; al reenviarlo desde una
     *                    DLQ por el exchange por defecto es el nombre de la cola
     */
    static Message recibido(String recibidaCon, String tipo, String cuerpo, String messageId) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propiedades.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        propiedades.setMessageId(messageId);
        propiedades.setTimestamp(new Date());
        propiedades.setType(tipo);
        propiedades.setReceivedExchange(RabbitTopologia.EXCHANGE);
        propiedades.setReceivedRoutingKey(recibidaCon);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }
}
