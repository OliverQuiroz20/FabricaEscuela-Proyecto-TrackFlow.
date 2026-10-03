package com.trackflow.modules.reports.api;

import com.trackflow.modules.reports.domain.PeriodoInvalidoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce los errores del periodo pedido. Se limita a este controlador para no
 * cambiar cómo responden los demás endpoints a un parámetro mal escrito.
 *
 * Va con la máxima precedencia porque, con spring.mvc.problemdetails.enabled, Spring
 * Boot registra su propio manejador de errores de conversión y, si llega primero, la
 * respuesta solo dice "Failed to convert". Al estar limitado a este controlador, la
 * precedencia no afecta a ningún otro endpoint.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ReporteVolumenController.class)
public class ReporteVolumenExceptionHandler {

    @ExceptionHandler(PeriodoInvalidoException.class)
    ProblemDetail periodoInvalido(PeriodoInvalidoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Periodo inválido");
        return problema;
    }

    /** Sin esto, una fecha mal escrita solo dice "Failed to convert value". */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail fechaIlegible(MethodArgumentTypeMismatchException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "'%s' no es una fecha válida para '%s'; use el formato AAAA-MM-DD".formatted(e.getValue(),
                        e.getName()));
        problema.setTitle("Fecha inválida");
        return problema;
    }
}
