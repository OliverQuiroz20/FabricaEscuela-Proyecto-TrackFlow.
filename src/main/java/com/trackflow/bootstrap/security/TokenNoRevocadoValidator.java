package com.trackflow.bootstrap.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rechaza los tokens de sesiones ya cerradas. Se suma a las validaciones por defecto
 * (firma y vencimiento), así que un token revocado responde 401 igual que uno vencido.
 */
class TokenNoRevocadoValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESION_CERRADA = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN, "La sesión de este token fue cerrada", null);

    private final TokenRevocadoRepository revocados;

    TokenNoRevocadoValidator(TokenRevocadoRepository revocados) {
        this.revocados = revocados;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String jti = token.getId();
        if (jti != null && revocados.existsById(jti)) {
            return OAuth2TokenValidatorResult.failure(SESION_CERRADA);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
