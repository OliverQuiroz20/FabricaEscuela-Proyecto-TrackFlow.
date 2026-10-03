package com.trackflow.modules.logistics.api.dto;

import com.trackflow.modules.logistics.domain.LogisticsEvent;
import java.util.List;

/**
 * Historial de un envío (HU-04): sus movimientos del más antiguo al más reciente.
 *
 * El mensaje distingue un envío que existe pero aún no se ha movido de una lista
 * vacía sin explicación; el número inexistente no llega aquí, responde 404.
 * Los campos van en inglés, igual que TrackingResponse (bug #89).
 */
public record HistorialResponse(
        String trackingNumber,
        int totalMovements,
        String message,
        List<EventoLogisticoResponse> movements) {

    static final String SIN_MOVIMIENTOS = "El envío todavía no registra movimientos";

    public static HistorialResponse from(String trackingNumber, List<LogisticsEvent> eventos) {
        return new HistorialResponse(
                trackingNumber,
                eventos.size(),
                eventos.isEmpty() ? SIN_MOVIMIENTOS : null,
                eventos.stream().map(EventoLogisticoResponse::from).toList());
    }
}
