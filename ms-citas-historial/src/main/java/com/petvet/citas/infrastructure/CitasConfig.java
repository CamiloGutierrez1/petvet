package com.petvet.citas.infrastructure;

import com.petvet.citas.domain.*;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CitasConfig {
    @Bean
    public Clock clock(@Value("${petvet.agenda.zona}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    @Bean
    public VacunacionStrategy esquemaCachorro() {
        return new EsquemaCachorro();
    }

    @Bean
    public VacunacionStrategy esquemaAdulto() {
        return new EsquemaAdulto();
    }

    @Bean
    public VacunacionStrategy esquemaGeriatrico() {
        return new EsquemaGeriatrico();
    }

    @Bean
    public SelectorEsquemaVacunacion selectorEsquemaVacunacion(List<VacunacionStrategy> esquemas) {
        return new SelectorEsquemaVacunacion(esquemas);
    }
}
