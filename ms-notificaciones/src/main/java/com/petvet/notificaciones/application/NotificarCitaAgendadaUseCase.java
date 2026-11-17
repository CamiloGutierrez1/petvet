package com.petvet.notificaciones.application;

import com.petvet.notificaciones.application.eventos.CitaAgendada;
import com.petvet.notificaciones.domain.TipoNotificacion;
import org.springframework.stereotype.Service;

/** Confirma al dueño la cita recién agendada. */
@Service
public class NotificarCitaAgendadaUseCase {
    private final PlantillasCorreo plantillas;
    private final EnvioSinDuplicados envio;

    public NotificarCitaAgendadaUseCase(PlantillasCorreo plantillas, EnvioSinDuplicados envio) {
        this.plantillas = plantillas;
        this.envio = envio;
    }

    /** @return falso si el mensaje ya se había procesado */
    public boolean ejecutar(String messageId, CitaAgendada cita) {
        return envio.enviar(messageId, TipoNotificacion.CITA_AGENDADA, cita.duenoEmail(),
                plantillas.citaAgendada(cita));
    }
}
