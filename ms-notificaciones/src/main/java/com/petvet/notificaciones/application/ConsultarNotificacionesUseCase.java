package com.petvet.notificaciones.application;

import com.petvet.notificaciones.domain.Notificacion;
import com.petvet.notificaciones.domain.NotificacionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConsultarNotificacionesUseCase {
    public static final int LIMITE_MAXIMO = 100;

    private final NotificacionRepository notificaciones;

    public ConsultarNotificacionesUseCase(NotificacionRepository notificaciones) {
        this.notificaciones = notificaciones;
    }

    /** Las últimas notificaciones enviadas, de la más reciente a la más antigua. */
    public List<Notificacion> recientes(int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("El límite debe estar entre 1 y " + LIMITE_MAXIMO);
        }
        return notificaciones.recientes(limite);
    }
}
