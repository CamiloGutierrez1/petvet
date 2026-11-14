package com.petvet.mascotas.infrastructure;

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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept")
                .allowCredentials(false);
    }

    @Bean
    public OpenAPI serviceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PetVet — Mascotas y Dueños")
                .version("0.1.0")
                .description("Dueños, mascotas y fotos de PetVet. GET /api/mascotas/{id} entrega la ficha plana "
                        + "que consume Citas. Las fotos subidas se guardan en PostgreSQL y se sirven en "
                        + "GET /api/mascotas/{id}/foto."));
    }
}
