package com.trackflow.modules.reports.api.dto;

import com.trackflow.modules.reports.application.VolumenDeEnvios;
import com.trackflow.modules.reports.application.VolumenPorPunto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record VolumenDeEnviosResponse(
        LocalDate from,
        LocalDate to,

        /** Instante hasta el que se contó. Si el periodo incluye hoy, es el momento de la consulta. */
        Instant cutoff,

        /** true si el periodo incluye el día en curso y la cifra aún puede crecer. */
        boolean inProgress,

        /** Envíos registrados en el periodo. */
        long totalShipments,

        /** Envíos entregados en el periodo (cifra aparte: pueden haberse registrado antes). */
        long totalDelivered,

        /** Desglose de totalShipments por punto de ingreso a la red; suma totalShipments. */
        List<PuntoResponse> byPoint,

        /** Solo viene informado cuando no hubo envíos en el periodo; si no, es null. */
        String message) {

    static final String SIN_ENVIOS = "No hay envíos registrados en el periodo consultado";

    public record PuntoResponse(Long centerId, String point, String city, boolean pendingReception,
            long shipments) {

        static PuntoResponse from(VolumenPorPunto punto) {
            return new PuntoResponse(punto.centroId(), punto.punto(), punto.ciudad(), punto.pendienteDeRecepcion(),
                    punto.envios());
        }
    }

    public static VolumenDeEnviosResponse from(VolumenDeEnvios volumen) {
        return new VolumenDeEnviosResponse(
                volumen.desde(),
                volumen.hasta(),
                volumen.corte(),
                volumen.enCurso(),
                volumen.totalRegistrados(),
                volumen.totalEntregados(),
                volumen.porPunto().stream().map(PuntoResponse::from).toList(),
                volumen.hayEnvios() ? null : SIN_ENVIOS);
    }
}
