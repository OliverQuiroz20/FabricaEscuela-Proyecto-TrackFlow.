package com.trackflow.modules.logistics.api.dto;

import com.trackflow.modules.logistics.domain.LogisticsEvent;
import java.time.Instant;

/** Un movimiento del historial. Campos en inglés, igual que TrackingResponse (bug #89). */
public record EventoLogisticoResponse(
        Long id,
        String trackingNumber,
        String type,
        String resultingStatus,
        Long centerId,
        String point,
        String cityName,
        String notes,
        String delivererName,
        Instant occurredAt,
        Instant registeredAt) {

    public static EventoLogisticoResponse from(LogisticsEvent event) {
        return new EventoLogisticoResponse(
                event.getId(),
                event.getTrackingNumber(),
                event.getType().name(),
                event.getType().resultingStatus(),
                event.getCenterId(),
                event.getPoint(),
                event.getCityName(),
                event.getNotes(),
                event.getDelivererName(),
                event.getOccurredAt(),
                event.getRegisteredAt());
    }
}
