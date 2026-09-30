package com.trackflow.bootstrap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * La escritura exige un token de operador; la consulta es pública, porque el número de
 * seguimiento es la credencial del cliente, como en cualquier servicio de paquetería.
 *
 * Sin credenciales configuradas la protección queda desactivada, de modo que el entorno
 * local y las pruebas no necesitan token y el despliegue sí lo exige.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final SecurityProperties propiedades;

    public SecurityConfig(SecurityProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean protegido = propiedades.proteccionActiva();

        http
                // Sin esto, el filtro de seguridad rechaza las peticiones preflight antes de
                // que se aplique la configuración CORS de Spring MVC (ver CorsConfig).
                .cors(Customizer.withDefaults())
                // CSRF explota que el navegador adjunta solo las cookies de sesión. Aquí no
                // hay cookies: la sesión es STATELESS y el JWT viaja en la cabecera
                // Authorization, que un sitio ajeno no puede añadir. Por eso se desactiva.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // El OPTIONS de precomprobación nunca lleva token, por diseño del propio
                    // CORS: si una regla de rol lo alcanza (como pasaba con /api/admin/**, que
                    // no distinguía el método), el navegador ve un 401 en la precomprobación y
                    // ni siquiera intenta la petición real. Dejarlo pasar aquí no abre nada: la
                    // petición real —GET, POST— sigue evaluándose por las reglas de abajo.
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/tracking/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/shipments/*/events").permitAll();
                    auth.requestMatchers("/", "/actuator/health/**", "/swagger-ui.html",
                            "/swagger-ui/**", "/v3/api-docs/**").permitAll();

                    if (protegido) {
                        // Las operaciones de mantenimiento no las hace quien registra paquetes.
                        auth.requestMatchers("/api/admin/**").hasRole(SecurityProperties.ROL_ADMIN);
                        auth.requestMatchers(HttpMethod.POST, "/api/**")
                                .hasRole(SecurityProperties.ROL_OPERADOR);
                    }

                    auth.anyRequest().permitAll();
                })
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeRoles())));

        if (protegido) {
            log.info("Seguridad activa: la escritura requiere token de operador (POST /api/auth/login)");
            if (!propiedades.adminConfigurado()) {
                log.warn("Sin clave de administrador: /api/admin/** queda inaccesible. "
                        + "Defina TRACKFLOW_ADMIN_CLAVE si necesita reconstruir proyecciones.");
            }
        } else {
            log.warn("Sin clave de operador configurada: los endpoints de escritura están abiertos. "
                    + "Defina TRACKFLOW_OPERADOR_CLAVE en cualquier entorno accesible desde internet.");
        }

        return http.build();
    }

    /** Traduce el claim "roles" del token a las autoridades que espera Spring Security. */
    private JwtAuthenticationConverter conversorDeRoles() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("roles");
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
