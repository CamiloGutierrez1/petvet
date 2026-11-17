package com.petvet.notificaciones.application;

import com.petvet.notificaciones.application.eventos.CitaCompletada;
import com.petvet.notificaciones.domain.TipoNotificacion;
import org.springframework.stereotype.Service;

/** Envía al dueño el resumen de la consulta, con los exámenes registrados si vienen en el evento. */
@Service
public class NotificarCitaCompletadaUseCase {
    private final PlantillasCorreo plantillas;
    private final EnvioSinDuplicados envio;

    public NotificarCitaCompletadaUseCase(PlantillasCorreo plantillas, EnvioSinDuplicados envio) {
        this.plantillas = plantillas;
        this.envio = envio;
    }

    /** @return falso si el mensaje ya se había procesado */
    public boolean ejecutar(String messageId, CitaCompletada cita) {
        return envio.enviar(messageId, TipoNotificacion.CITA_COMPLETADA, cita.duenoEmail(),
                plantillas.citaCompletada(cita));
    }
}
