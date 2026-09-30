package com.trackflow.bootstrap.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * administradores, y lo revoca al cerrar sesión.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
    private final TokenRevocadoRepository revocados;

    public AuthController(SecurityProperties propiedades, JwtEncoder jwtEncoder,
            TokenRevocadoRepository revocados) {
        this.propiedades = propiedades;
        this.jwtEncoder = jwtEncoder;
        this.revocados = revocados;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        if (!propiedades.proteccionActiva()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No hay credenciales configuradas en este entorno");
        }

        String roles = propiedades.rolesDe(request.usuario(), request.clave());
        if (roles == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o clave incorrectos");
        }

        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(propiedades.duracionToken());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                // El jti identifica esta sesión, para poder revocarla al cerrar sesión.
                .id(UUID.randomUUID().toString())
                .issuer("trackflow")
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .subject(request.usuario())
                .claim("roles", roles)
                .build();

        // Sin indicar el algoritmo, el codificador asume RS256 y no encuentra clave asimétrica.
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", roles, propiedades.duracionToken().toSeconds());
    }

    /** Quién tiene la sesión abierta con este token, para que la interfaz lo muestre. */
    @GetMapping("/sesion")
    public SesionResponse sesion(@AuthenticationPrincipal Jwt jwt) {
        return new SesionResponse(jwt.getSubject(), jwt.getClaimAsString("roles"), jwt.getExpiresAt());
    }

    /**
     * Cierra la sesión: desde ahora el token responde 401 aunque no haya vencido.
     * Llamarlo dos veces con el mismo token no es posible, porque la segunda ya no pasa
     * la autenticación.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        Instant ahora = Instant.now();
        revocados.borrarVencidos(ahora);

        // Los tokens emitidos antes de existir el cierre de sesión no llevan jti:
        // no se pueden revocar, pero vencen solos en menos de una hora.
        if (jwt.getId() != null) {
            revocados.save(new TokenRevocado(jwt.getId(), jwt.getSubject(), jwt.getExpiresAt(), ahora));
        }
    }
}
