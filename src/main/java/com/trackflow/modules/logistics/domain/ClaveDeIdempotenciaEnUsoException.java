package com.trackflow.modules.logistics.domain;

/**
 * La clave de idempotencia ya identifica un movimiento de otro envío. Reutilizarla no
 * puede devolver ese movimiento ajeno: sería mostrarle a un envío lo que le pasó a otro.
 */
public class ClaveDeIdempotenciaEnUsoException extends RuntimeException {

    public ClaveDeIdempotenciaEnUsoException(String clave, String trackingNumber) {
        super("La clave de idempotencia '%s' ya se usó para registrar un movimiento de otro envío, no de %s"
                .formatted(clave, trackingNumber));
    }
}
