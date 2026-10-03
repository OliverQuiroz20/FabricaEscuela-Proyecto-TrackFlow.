package com.trackflow.modules.logistics.domain;

/** La clave de idempotencia no cabe como identificador del evento. */
public class ClaveDeIdempotenciaInvalidaException extends RuntimeException {

    public ClaveDeIdempotenciaInvalidaException(int maximo) {
        super("La cabecera Idempotency-Key admite como máximo %d caracteres".formatted(maximo));
    }
}
