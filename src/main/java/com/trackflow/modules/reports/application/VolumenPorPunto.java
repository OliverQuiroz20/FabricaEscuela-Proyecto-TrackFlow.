package com.trackflow.modules.reports.application;

/**
 * Cuántos envíos registrados en el periodo entraron por un punto de la red.
 *
 * Si {@code pendienteDeRecepcion} es true, el envío aún no se ha recibido en ningún
 * centro: {@code centroId} es null y {@code punto}/{@code ciudad} describen su ciudad
 * de origen, que es donde se espera que ingrese.
 *
 * @param centroId id del centro del catálogo, o null si está pendiente o se reportó con texto libre
 */
public record VolumenPorPunto(
        Long centroId,
        String punto,
        String ciudad,
        boolean pendienteDeRecepcion,
        long envios) {
}
