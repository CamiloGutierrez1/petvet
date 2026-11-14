package com.petvet.notificaciones.application;

import com.petvet.notificaciones.domain.EnviadorNotificaciones;
import com.petvet.notificaciones.domain.Notificacion;
import com.petvet.notificaciones.domain.NotificacionRepository;
import com.petvet.notificaciones.domain.TipoNotificacion;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Envía cada mensaje de RabbitMQ una sola vez. La entrega es al menos una vez, así que el mismo
 * messageId puede llegar repetido.
 */
@Service
public class EnvioSinDuplicados {
    private static final Logger LOG = LoggerFactory.getLogger(EnvioSinDuplicados.class);

    private final NotificacionRepository notificaciones;
    private final EnviadorNotificaciones enviador;
    private final TransactionOperations transacciones;
    private final Clock clock;

    public EnvioSinDuplicados(NotificacionRepository notificaciones, EnviadorNotificaciones enviador,
            TransactionOperations transacciones, Clock clock) {
        this.notificaciones = notificaciones;
        this.enviador = enviador;
        this.transacciones = transacciones;
        this.clock = clock;
    }

    /**
     * En una sola transacción registra el messageId junto con la notificación y envía el correo de
     * último. Si el envío falla, la transacción se deshace y el mensaje se puede reintentar. Solo
     * queda un duplicado posible si el commit falla después de que el SMTP aceptó el correo.
     *
     * @return falso si el messageId ya se había procesado; en ese caso no envía nada
     */
    public boolean enviar(String messageId, TipoNotificacion tipo, String destino, MensajeCorreo correo) {
        Instant creadaEn = Instant.now(clock);
        boolean enviada = Boolean.TRUE.equals(transacciones.execute(estado -> {
            Notificacion notificacion = new Notificacion(null, messageId, tipo, destino, correo.asunto(),
                    correo.cuerpo(), creadaEn, Instant.now(clock));
            if (!notificaciones.registrarSiEsNueva(notificacion)) {
                return false;
            }
            enviador.enviar(destino, correo.asunto(), correo.cuerpo());
            return true;
        }));
        if (enviada) {
            LOG.info("Notificación {} enviada para el mensaje {}", tipo, messageId);
        } else {
            LOG.info("Mensaje {} repetido: la notificación {} ya se había enviado", messageId, tipo);
        }
        return enviada;
    }
}
