package com.petvet.mascotas.infrastructure;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MascotasConfig {
    /** Reloj de la clínica: la fecha de nacimiento no puede ser posterior a hoy en esta zona. */
    @Bean
    public Clock clock(@Value("${petvet.zona}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
