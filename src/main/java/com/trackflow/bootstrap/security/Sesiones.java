package com.trackflow.bootstrap.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida de las sesiones (HU-08): se abren al iniciar sesión, se cierran al
 * cerrar sesión, y se dan por terminadas si pasan más del tiempo configurado sin
 * actividad o si el usuario fue desactivado mientras tanto.
 */
@Service
public class Sesiones {

    private final SesionRepository sesiones;
    private final UsuarioRepository usuarios;
    private final SecurityProperties propiedades;
    private final Clock clock;

    public Sesiones(SesionRepository sesiones, UsuarioRepository usuarios, SecurityProperties propiedades,
            Clock clock) {
        this.sesiones = sesiones;
        this.usuarios = usuarios;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    @Transactional
    public void abrir(String jti, String username, Instant ahora, Instant expira) {
        sesiones.borrarVencidas(ahora);
        sesiones.save(new Sesion(jti, username, ahora, expira));
    }

    @Transactional
    public void cerrar(String jti) {
        sesiones.findById(jti).ifPresent(sesion -> sesion.cerrar(clock.instant()));
    }

    /**
     * Si la sesión del token sigue vigente, registra la actividad y devuelve true.
     * Una sesión inactiva o de un usuario desactivado se cierra en el acto, para que
     * no pueda volver a usarse aunque el token aún no haya vencido.
     */
    @Transactional
    public boolean validarYRenovar(String jti) {
        if (jti == null) {
            return false;
        }

        Optional<Sesion> encontrada = sesiones.findById(jti);
        if (encontrada.isEmpty() || !encontrada.get().abierta()) {
            return false;
        }

        Sesion sesion = encontrada.get();
        Instant ahora = clock.instant();

        boolean usuarioActivo = usuarios.findById(sesion.getUsername()).map(Usuario::isActive).orElse(false);
        if (!usuarioActivo || sesion.inactivaDesde(ahora, propiedades.inactividad())) {
            sesion.cerrar(ahora);
            return false;
        }

        sesion.registrarActividad(ahora);
        return true;
    }
}
