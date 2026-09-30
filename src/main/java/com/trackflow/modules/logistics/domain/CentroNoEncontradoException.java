package com.trackflow.modules.logistics.domain;

/**
 * No existe ningún centro con el id consultado. Es un 404 de lectura (GET
 * /api/centers/{id}), distinto de CentroInvalidoException: esa está reservada para
 * cuando un centro no sirve para admitir un evento (422, error semántico al escribir).
 */
public class CentroNoEncontradoException extends RuntimeException {

    public CentroNoEncontradoException(Long id) {
        super("No existe un centro con id " + id);
    }
}
