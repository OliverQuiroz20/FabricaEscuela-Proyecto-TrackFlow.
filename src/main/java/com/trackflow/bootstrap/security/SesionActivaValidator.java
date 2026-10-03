package com.trackflow.bootstrap.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Exige que la sesión del token siga abierta en el servidor. Se suma a las
 * validaciones por defecto (firma y vencimiento), así que un token de una sesión
 * cerrada, inactiva o de un usuario desactivado responde 401 igual que uno vencido.
 */
class SesionActivaValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESION_TERMINADA = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
            "La sesión fue cerrada o expiró por inactividad; inicie sesión de nuevo", null);

    private final Sesiones sesiones;

    SesionActivaValidator(Sesiones sesiones) {
        this.sesiones = sesiones;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return sesiones.validarYRenovar(token.getId())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(SESION_TERMINADA);
    }
}
