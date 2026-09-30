package com.trackflow.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Solo los orígenes listados en TRACKFLOW_CORS_ORIGINS pueden llamar a la API desde un
 * navegador. Antes se aceptaba cualquier origen ("*"), que SonarCloud marcó como
 * hotspot de seguridad: cualquier sitio web podía invocar la API.
 */
@Configuration
public class CorsConfig {

    private final String[] origenesPermitidos;

    public CorsConfig(@Value("${trackflow.cors.allowed-origins}") String[] origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(origenesPermitidos)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }
}
