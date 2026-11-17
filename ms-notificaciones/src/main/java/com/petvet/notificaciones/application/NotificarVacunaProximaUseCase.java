package com.petvet.notificaciones.application;

import com.petvet.notificaciones.application.eventos.VacunaProxima;
import com.petvet.notificaciones.domain.TipoNotificacion;
import org.springframework.stereotype.Service;

/** Recuerda al dueño la vacuna que está por vencer. */
@Service
public class NotificarVacunaProximaUseCase {
    private final PlantillasCorreo plantillas;
    private final EnvioSinDuplicados envio;

    public NotificarVacunaProximaUseCase(PlantillasCorreo plantillas, EnvioSinDuplicados envio) {
        this.plantillas = plantillas;
        this.envio = envio;
    }

    /** @return falso si el mensaje ya se había procesado */
    public boolean ejecutar(String messageId, VacunaProxima aviso) {
        return envio.enviar(messageId, TipoNotificacion.VACUNA_PROXIMA, aviso.duenoEmail(),
                plantillas.vacunaProxima(aviso));
    }
}
