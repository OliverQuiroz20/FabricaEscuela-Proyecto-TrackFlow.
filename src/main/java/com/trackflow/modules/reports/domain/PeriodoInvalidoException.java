package com.trackflow.modules.reports.domain;

/**
 * El periodo pedido no se puede consultar: está invertido o empieza en el futuro. El
 * reporte describe lo que ya pasó; proyectar volumen futuro está fuera de alcance.
 */
public class PeriodoInvalidoException extends RuntimeException {

    public PeriodoInvalidoException(String message) {
        super(message);
    }
}
