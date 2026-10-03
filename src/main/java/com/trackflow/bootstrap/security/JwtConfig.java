package com.trackflow.bootstrap.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Firma y verificación de los tokens con un secreto compartido (HMAC-SHA256).
 *
 * Se eligió firma simétrica porque quien emite y quien valida son la misma aplicación.
 * Si en el futuro otros servicios tuvieran que validar tokens sin poder firmarlos,
 * habría que pasar a un par de claves asimétricas.
 */
@Configuration
public class JwtConfig {

    private final SecretKey clave;

    public JwtConfig(SecurityProperties propiedades) {
        this.clave = new SecretKeySpec(
                propiedades.secretoEfectivo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** BCrypt guarda la sal dentro del hash: no hace falta una columna aparte para ella. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    /**
     * A las validaciones por defecto (firma y vencimiento) se suma la de sesión activa
     * (HU-08): sin ella, un token seguiría sirviendo después del logout, de la
     * inactividad o de desactivar al usuario, hasta que venciera.
     */
    @Bean
    JwtDecoder jwtDecoder(Sesiones sesiones) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), new SesionActivaValidator(sesiones)));
        return decoder;
    }
}
