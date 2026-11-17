package com.petvet.notificaciones.infrastructure.messaging;

import com.petvet.notificaciones.application.NotificarCitaAgendadaUseCase;
import com.petvet.notificaciones.application.NotificarCitaCompletadaUseCase;
import com.petvet.notificaciones.application.eventos.CitaAgendada;
import com.petvet.notificaciones.application.eventos.CitaCompletada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Único consumidor de notificaciones.citas. Por la cola llegan dos formatos y Citas no manda
 * __TypeId__, así que el caso de uso se elige por la routing key.
 */
@Component
public class CitasListener {
    private static final Logger LOG = LoggerFactory.getLogger(CitasListener.class);

    private final NotificarCitaAgendadaUseCase citaAgendada;
    private final NotificarCitaCompletadaUseCase citaCompletada;

    public CitasListener(NotificarCitaAgendadaUseCase citaAgendada, NotificarCitaCompletadaUseCase citaCompletada) {
        this.citaAgendada = citaAgendada;
        this.citaCompletada = citaCompletada;
    }

    @RabbitListener(queues = RabbitTopologia.COLA_CITAS)
    public void recibir(Message mensaje) {
        String clave = LectorEventos.clave(mensaje);
        switch (clave) {
            case RabbitTopologia.CITA_AGENDADA -> citaAgendada.ejecutar(LectorEventos.messageId(mensaje),
                    LectorEventos.leer(mensaje, CitaAgendada.class));
            case RabbitTopologia.CITA_COMPLETADA -> citaCompletada.ejecutar(LectorEventos.messageId(mensaje),
                    LectorEventos.leer(mensaje, CitaCompletada.class));
            case null, default -> LOG.warn("Se descarta el mensaje {} de {}: la clave {} no genera notificación",
                    mensaje.getMessageProperties().getMessageId(), RabbitTopologia.COLA_CITAS, clave);
        }
    }
}
