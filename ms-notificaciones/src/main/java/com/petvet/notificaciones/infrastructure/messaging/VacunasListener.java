package com.petvet.notificaciones.infrastructure.messaging;

import com.petvet.notificaciones.application.NotificarVacunaProximaUseCase;
import com.petvet.notificaciones.application.eventos.VacunaProxima;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Único consumidor de notificaciones.vacunas. */
@Component
public class VacunasListener {
    private static final Logger LOG = LoggerFactory.getLogger(VacunasListener.class);

    private final NotificarVacunaProximaUseCase vacunaProxima;

    public VacunasListener(NotificarVacunaProximaUseCase vacunaProxima) {
        this.vacunaProxima = vacunaProxima;
    }

    @RabbitListener(queues = RabbitTopologia.COLA_VACUNAS)
    public void recibir(Message mensaje) {
        String clave = LectorEventos.clave(mensaje);
        switch (clave) {
            case RabbitTopologia.VACUNA_PROXIMA -> vacunaProxima.ejecutar(LectorEventos.messageId(mensaje),
                    LectorEventos.leer(mensaje, VacunaProxima.class));
            case null, default -> LOG.warn("Se descarta el mensaje {} de {}: la clave {} no genera notificación",
                    mensaje.getMessageProperties().getMessageId(), RabbitTopologia.COLA_VACUNAS, clave);
        }
    }
}
