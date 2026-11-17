package com.petvet.notificaciones.infrastructure;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificacionesConfig {

    /** Hora de la clínica: decide si una vacuna ya venció y fecha las notificaciones guardadas. */
    @Bean
    public Clock clock(@Value("${petvet.notificaciones.zona}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
