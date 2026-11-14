package com.petvet.notificaciones.domain;

import java.util.List;

public interface NotificacionRepository {

    /**
     * Guarda la notificación si su messageId no se había procesado. Devuelve falso si es un
     * duplicado; en ese caso no guarda nada. Debe llamarse dentro de una transacción.
     */
    boolean registrarSiEsNueva(Notificacion notificacion);

    /** Las últimas notificaciones enviadas, de la más reciente a la más antigua. */
    List<Notificacion> recientes(int limite);
}
