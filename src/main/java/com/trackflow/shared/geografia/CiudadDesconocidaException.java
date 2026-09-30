package com.trackflow.shared.geografia;

/**
 * La ciudad indicada no está en el catálogo.
 */
public class CiudadDesconocidaException extends RuntimeException {

    private final Long id;

    public CiudadDesconocidaException(Long id) {
        super(("La ciudad %s no está en el catálogo. "
                + "Consulte GET /api/cities?q= para obtener los identificadores válidos")
                .formatted(id));
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
