package com.trackflow.bootstrap.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Inicio y cierre de sesión (HU-08). Emite el token que necesitan operadores y
 * administradores, abre su sesión en el servidor y la cierra al cerrar sesión.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Mismo mensaje para usuario inexistente, clave incorrecta y usuario desactivado:
     * distinguirlos le diría a un atacante qué usuarios existen.
     */
    static final String CREDENCIALES_INVALIDAS = "Usuario o clave incorrectos";

    public record LoginRequest(
            @NotBlank(message = "el usuario es obligatorio") String usuario,
            @NotBlank(message = "la clave es obligatoria") String clave) {
    }

    public record TokenResponse(String token, String tipo, String roles, long expiraEnSegundos) {
    }

    public record SesionResponse(String username, String roles, Instant expiresAt) {
    }

    private final SecurityProperties propiedades;
    private final JwtEncoder jwtEncoder;
    private final UsuarioRepository usuarios;
    private final Sesiones sesiones;
    private final PasswordEncoder encoder;
    private final Clock clock;

    /**
     * Hash contra el que se compara cuando el usuario no existe. Sin él, el login de
     * un usuario inexistente respondería más rápido (BCrypt tarda a propósito) y el
     * tiempo de respuesta revelaría qué usuarios existen.
     */
    private final String hashFicticio;

    public AuthController(SecurityProperties propiedades, JwtEncoder jwtEncoder, UsuarioRepository usuarios,
            Sesiones sesiones, PasswordEncoder encoder, Clock clock) {
        this.propiedades = propiedades;
        this.jwtEncoder = jwtEncoder;
        this.usuarios = usuarios;
        this.sesiones = sesiones;
        this.encoder = encoder;
        this.clock = clock;
        this.hashFicticio = encoder.encode(UUID.randomUUID().toString());
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        if (!propiedades.proteccionActiva()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No hay credenciales configuradas en este entorno");
        }

        Optional<Usuario> encontrado = usuarios.findById(request.usuario());
        // Se compara siempre, exista o no el usuario y esté o no activo, para que los
        // tres casos de rechazo tarden lo mismo.
        boolean claveCorrecta = encoder.matches(request.clave(),
                encontrado.map(Usuario::getPasswordHash).orElse(hashFicticio));

        if (encontrado.isEmpty() || !claveCorrecta || !encontrado.get().isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        Usuario usuario = encontrado.get();
        Instant ahora = clock.instant();
        Instant expiracion = ahora.plus(propiedades.duracionToken());
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                // El jti identifica la sesión en el servidor (ver Sesiones).
                .id(jti)
                .issuer("trackflow")
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .subject(usuario.getUsername())
                .claim("roles", usuario.getRoles())
                .build();

        // Sin indicar el algoritmo, el codificador asume RS256 y no encuentra clave asimétrica.
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();

        sesiones.abrir(jti, usuario.getUsername(), ahora, expiracion);

        return new TokenResponse(token, "Bearer", usuario.getRoles(), propiedades.duracionToken().toSeconds());
    }

    /** Quién tiene la sesión abierta con este token, para que la interfaz lo muestre. */
    @GetMapping("/sesion")
    public SesionResponse sesion(@AuthenticationPrincipal Jwt jwt) {
        return new SesionResponse(jwt.getSubject(), jwt.getClaimAsString("roles"), jwt.getExpiresAt());
    }

    /**
     * Cierra la sesión en el servidor: desde ahora el token responde 401 aunque no
     * haya vencido. Llamarlo dos veces con el mismo token no es posible, porque la
     * segunda ya no pasa la autenticación.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        sesiones.cerrar(jwt.getId());
    }
}
