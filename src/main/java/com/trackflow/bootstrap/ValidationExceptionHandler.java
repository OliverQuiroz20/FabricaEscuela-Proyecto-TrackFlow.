package com.trackflow.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Detalla qué campos obligatorios faltan, como piden los criterios de aceptación.
 * La respuesta por defecto solo dice "Invalid request content", que no le sirve
 * a quien está registrando el envío.
 *
 * Es transversal a todos los módulos y no depende de ninguno: solo traduce los
 * errores de Bean Validation.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail camposInvalidos(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Datos obligatorios incompletos");
        problema.setDetail("La solicitud no se puede procesar porque faltan datos obligatorios");
        problema.setProperty("missingFields", campos);

        return problema;
    }

    /**
     * Un valor fuera del catálogo (por ejemplo un tipo de documento inexistente)
     * rompe la deserialización antes de llegar a Bean Validation. Sin esto la
     * respuesta solo dice "Failed to read request".
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail cuerpoIlegible(HttpMessageNotReadableException e) {
        String causa = e.getMostSpecificCause().getMessage();

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Valor no admitido");
        problema.setDetail("Alguno de los campos trae un valor que no se puede interpretar");
        problema.setProperty("cause", causa);

        return problema;
    }
}
