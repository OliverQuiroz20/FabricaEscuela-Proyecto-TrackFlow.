package com.trackflow.bootstrap.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuración de seguridad tomada del entorno.
 *
 * Si no se configura contraseña de operador, la protección queda desactivada: es lo
 * cómodo en local y para las pruebas, pero nunca debe ocurrir en un entorno accesible
 * desde internet. El arranque lo advierte cuando pasa.
 */
@Component
public class SecurityProperties {

    private static final Logger log = LoggerFactory.getLogger(SecurityProperties.class);

    /** Se reutiliza: crear un SecureRandom por llamada es costoso y no mejora la aleatoriedad. */
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public static final String ROL_OPERADOR = "OPERADOR";
    public static final String ROL_ADMIN = "ADMIN";

    private final String secreto;
    private final String operadorUsuario;
    private final String operadorClave;
    private final String adminUsuario;
    private final String adminClave;
    private final Duration duracionToken;
    private final Duration inactividad;

    public SecurityProperties(
            @Value("${trackflow.security.jwt-secret:}") String secreto,
            @Value("${trackflow.security.operador-usuario:operador}") String operadorUsuario,
            @Value("${trackflow.security.operador-clave:}") String operadorClave,
            @Value("${trackflow.security.admin-usuario:admin}") String adminUsuario,
            @Value("${trackflow.security.admin-clave:}") String adminClave,
            @Value("${trackflow.security.duracion-token-minutos:480}") long duracionMinutos,
            @Value("${trackflow.security.inactividad-minutos:30}") long inactividadMinutos) {
        this.secreto = secreto;
        this.operadorUsuario = operadorUsuario;
        this.operadorClave = operadorClave;
        this.adminUsuario = adminUsuario;
        this.adminClave = adminClave;
        this.duracionToken = Duration.ofMinutes(duracionMinutos);
        this.inactividad = Duration.ofMinutes(inactividadMinutos);
    }

    public boolean proteccionActiva() {
        return !esVacia(operadorClave) || !esVacia(adminClave);
    }

    public String operadorUsuario() {
        return operadorUsuario;
    }

    /** Solo para crear el usuario al arrancar: el login compara contra el hash guardado. */
    public String operadorClave() {
        return operadorClave;
    }

    public String adminUsuario() {
        return adminUsuario;
    }

    public String adminClave() {
        return adminClave;
    }

    /**
     * Si no hay secreto configurado se genera uno aleatorio al arrancar. Los tokens
     * emitidos dejan de ser válidos al reiniciar, lo que en desarrollo es aceptable
     * y en producción obliga a configurarlo de verdad.
     */
    public String secretoEfectivo() {
        if (secreto != null && secreto.length() >= 32) {
            return secreto;
        }

        if (proteccionActiva()) {
            log.warn("Sin 'trackflow.security.jwt-secret' de al menos 32 caracteres: se genera uno "
                    + "aleatorio y los tokens se invalidarán en cada reinicio");
        }

        byte[] aleatorio = new byte[32];
        ALEATORIO.nextBytes(aleatorio);
        return Base64.getEncoder().encodeToString(aleatorio);
    }

    public boolean adminConfigurado() {
        return !esVacia(adminClave);
    }

    /** Vigencia máxima del token, aunque la sesión siga en uso. */
    public Duration duracionToken() {
        return duracionToken;
    }

    /** Tiempo sin actividad tras el que la sesión se cierra (HU-08). */
    public Duration inactividad() {
        return inactividad;
    }

    private static boolean esVacia(String valor) {
        return valor == null || valor.isBlank();
    }
}
