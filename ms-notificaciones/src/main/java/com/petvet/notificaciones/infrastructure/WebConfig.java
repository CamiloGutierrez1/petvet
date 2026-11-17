package com.petvet.notificaciones.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfig(@Value("${petvet.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /** La API de Notificaciones es de solo lectura. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept")
                .allowCredentials(false);
    }

    @Bean
    public OpenAPI serviceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PetVet — Notificaciones")
                .version("0.1.0")
                .description("Consume de RabbitMQ los eventos de Citas (cita.agendada, cita.completada y "
                        + "vacuna.proxima) y envía un correo por cada uno, sin repetirlo aunque el mensaje llegue "
                        + "dos veces. En local los correos llegan a MailHog. Esta API solo consulta lo enviado."));
    }
}
