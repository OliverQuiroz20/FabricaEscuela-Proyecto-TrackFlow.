package com.trackflow.bootstrap.security;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea al arrancar los usuarios operador y administrador a partir de las claves
 * del entorno (TRACKFLOW_OPERADOR_CLAVE y TRACKFLOW_ADMIN_CLAVE), guardando solo el
 * hash. Sin ellos no habría con quién entrar: crear usuarios desde la API es la HU-12.
 *
 * Si la clave del entorno cambia, se actualiza el hash; así rotar una clave sigue
 * siendo cambiar una variable en Render. Lo que no se toca es si el usuario está
 * activo: si alguien lo desactivó, reiniciar no lo reactiva.
 */
@Component
public class UsuariosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuariosIniciales.class);

    private final UsuarioRepository usuarios;
    private final SecurityProperties propiedades;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public UsuariosIniciales(UsuarioRepository usuarios, SecurityProperties propiedades, PasswordEncoder encoder,
            Clock clock) {
        this.usuarios = usuarios;
        this.propiedades = propiedades;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        asegurar(propiedades.operadorUsuario(), propiedades.operadorClave(), SecurityProperties.ROL_OPERADOR);
        // El administrador también es operador: hereda sus permisos.
        asegurar(propiedades.adminUsuario(), propiedades.adminClave(),
                SecurityProperties.ROL_ADMIN + " " + SecurityProperties.ROL_OPERADOR);
    }

    private void asegurar(String username, String clave, String roles) {
        if (clave == null || clave.isBlank()) {
            return;
        }

        usuarios.findById(username).ifPresentOrElse(
                usuario -> {
                    if (!encoder.matches(clave, usuario.getPasswordHash())) {
                        usuario.cambiarPasswordHash(encoder.encode(clave));
                        log.info("Clave del usuario '{}' actualizada desde el entorno", username);
                    }
                },
                () -> {
                    usuarios.save(new Usuario(username, encoder.encode(clave), roles, true, clock.instant()));
                    log.info("Usuario '{}' creado con roles {}", username, roles);
                });
    }
}
