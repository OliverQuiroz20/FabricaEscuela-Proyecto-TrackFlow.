package com.trackflow.bootstrap.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    public SecurityProperties(
            @Value("${trackflow.security.jwt-secret:}") String secreto,
            @Value("${trackflow.security.operador-usuario:operador}") String operadorUsuario,
            @Value("${trackflow.security.operador-clave:}") String operadorClave,
            @Value("${trackflow.security.admin-usuario:admin}") String adminUsuario,
            @Value("${trackflow.security.admin-clave:}") String adminClave,
            @Value("${trackflow.security.duracion-token-minutos:60}") long duracionMinutos) {
        this.secreto = secreto;
        this.operadorUsuario = operadorUsuario;
        this.operadorClave = operadorClave;
        this.adminUsuario = adminUsuario;
        this.adminClave = adminClave;
        this.duracionToken = Duration.ofMinutes(duracionMinutos);
    }

    public boolean proteccionActiva() {
        return !esVacia(operadorClave) || !esVacia(adminClave);
    }

    /**
     * Roles que corresponden a unas credenciales, o {@code null} si no son válidas.
     * El administrador también es operador: puede hacer todo lo que hace este.
     */
    public String rolesDe(String usuario, String clave) {
        if (!esVacia(adminClave) && coincide(adminUsuario, usuario) && coincide(adminClave, clave)) {
            return ROL_ADMIN + " " + ROL_OPERADOR;
        }
        if (!esVacia(operadorClave) && coincide(operadorUsuario, usuario) && coincide(operadorClave, clave)) {
            return ROL_OPERADOR;
        }
        return null;
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

    public Duration duracionToken() {
        return duracionToken;
    }

    private static boolean esVacia(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Comparación de tiempo constante, para no filtrar información por lo que tarda. */
    private static boolean coincide(String esperado, String recibido) {
        if (recibido == null) {
            return false;
        }
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8));
    }
}
