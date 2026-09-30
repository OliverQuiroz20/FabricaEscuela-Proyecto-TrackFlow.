package com.trackflow.bootstrap.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Emite el token que necesitan operadores y administradores.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(
            @NotBlank(message = "el usuario es obligatorio") String username,
            @NotBlank(message = "la clave es obligatoria") String password) {
    }

    public record TokenResponse(String token, String type, String roles, long expiresInSeconds) {
    }

    private final SecurityProperties propiedades;
    private final JwtEncoder jwtEncoder;

    public AuthController(SecurityProperties propiedades, JwtEncoder jwtEncoder) {
        this.propiedades = propiedades;
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        if (!propiedades.proteccionActiva()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No hay credenciales configuradas en este entorno");
        }

        String roles = propiedades.rolesDe(request.username(), request.password());
        if (roles == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o clave incorrectos");
        }

        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(propiedades.duracionToken());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("trackflow")
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .subject(request.username())
                .claim("roles", roles)
                .build();

        // Sin indicar el algoritmo, el codificador asume RS256 y no encuentra clave asimétrica.
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", roles, propiedades.duracionToken().toSeconds());
    }
}
