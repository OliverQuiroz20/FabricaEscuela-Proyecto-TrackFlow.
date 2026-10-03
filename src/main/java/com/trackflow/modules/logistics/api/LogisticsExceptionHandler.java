package com.trackflow.modules.logistics.api;

import com.trackflow.modules.logistics.domain.CentroFueraDeCiudadException;
import com.trackflow.modules.logistics.domain.CentroInvalidoException;
import com.trackflow.modules.logistics.domain.CentroNoEncontradoException;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaEnUsoException;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaInvalidaException;
import com.trackflow.modules.logistics.domain.MovimientoFueraDeOrdenException;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del dominio a respuestas HTTP. Vive en api/ para que el
 * dominio no tenga que conocer el protocolo.
 */
@RestControllerAdvice(assignableTypes = { LogisticsEventController.class, CentrosController.class })
public class LogisticsExceptionHandler {

    @ExceptionHandler(UnknownShipmentException.class)
    ProblemDetail envioDesconocido(UnknownShipmentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CentroNoEncontradoException.class)
    ProblemDetail centroNoEncontrado(CentroNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(FechaDeMovimientoInvalidaException.class)
    ProblemDetail fechaInvalida(FechaDeMovimientoInvalidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Fecha del movimiento inválida");
        return problema;
    }

    @ExceptionHandler(CentroInvalidoException.class)
    ProblemDetail centroInvalido(CentroInvalidoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("Centro inválido");
        return problema;
    }

    @ExceptionHandler(CentroFueraDeCiudadException.class)
    ProblemDetail centroFueraDeCiudad(CentroFueraDeCiudadException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("El centro no está en la ciudad esperada");
        return problema;
    }

    @ExceptionHandler(ClaveDeIdempotenciaInvalidaException.class)
    ProblemDetail claveInvalida(ClaveDeIdempotenciaInvalidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Clave de idempotencia inválida");
        return problema;
    }

    @ExceptionHandler(ClaveDeIdempotenciaEnUsoException.class)
    ProblemDetail claveEnUso(ClaveDeIdempotenciaEnUsoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setTitle("Clave de idempotencia ya usada en otro envío");
        return problema;
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    ProblemDetail transicionInvalida(TransicionInvalidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("Movimiento fuera del recorrido");
        return problema;
    }

    @ExceptionHandler(MovimientoFueraDeOrdenException.class)
    ProblemDetail movimientoFueraDeOrden(MovimientoFueraDeOrdenException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("Movimiento anterior al último registrado");
        return problema;
    }
}
