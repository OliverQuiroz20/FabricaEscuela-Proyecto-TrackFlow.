package com.trackflow.modules.reports.api;

import com.trackflow.modules.reports.domain.TrackingNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del módulo a respuestas HTTP con el mismo formato
 * (ProblemDetail: title, detail, status, instance) que usan los demás módulos.
 * Antes este 404 se armaba a mano con otros campos (timestamp, error, message), y
 * quien consumía la API tenía que leer los errores de dos formas distintas.
 */
@RestControllerAdvice(assignableTypes = ConsultaEnvioController.class)
public class ReportsExceptionHandler {

    @ExceptionHandler(TrackingNotFoundException.class)
    ProblemDetail envioNoEncontrado(TrackingNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
