package com.trackflow.modules.logistics.domain;

/**
 * El centro indicado no sirve para registrar un evento: no existe, o existe pero
 * está inactivo. Es un 422: la petición está bien formada, pero el centro que pide no
 * se puede usar.
 */
public class CentroInvalidoException extends RuntimeException {

    private CentroInvalidoException(String mensaje) {
        super(mensaje);
    }

    public static CentroInvalidoException noExiste(Long centroId) {
        return new CentroInvalidoException(
                "El centro %d no existe. Consulte GET /api/centers?q= para ver los centros disponibles"
                        .formatted(centroId));
    }

    public static CentroInvalidoException inactivo(Long centroId, String nombre) {
        return new CentroInvalidoException(
                "El centro '%s' (id %d) está inactivo y no puede usarse para registrar movimientos"
                        .formatted(nombre, centroId));
    }
}
