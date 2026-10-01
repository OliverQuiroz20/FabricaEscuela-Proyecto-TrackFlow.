package com.trackflow.shared.events;

import java.time.Instant;

/**
 * Evento de integración: lo publica logistics y lo consumen shipments y reports.
 * Vive en shared porque es el contrato entre módulos, no el modelo interno de ninguno.
 *
 * Distingue tres momentos, y confundirlos es lo que hacía retroceder el estado del
 * envío: {@code movedAt} es cuándo ocurrió el movimiento, {@code registeredAt} cuándo
 * se recibió el reporte y {@code occurredAt} cuándo se publicó este evento.
 *
 * {@code centerId} y {@code cityName} identifican el centro del catálogo donde ocurrió
 * el movimiento, para que quien agrupe por punto de la red no dependa del nombre
 * (que puede repetirse o cambiar). Son null en los eventos históricos registrados con
 * texto libre, antes de que existiera el catálogo de centros.
 */
public record EventoLogisticoRegistradoEvent(
        Long logisticsEventId,
        String trackingNumber,
        String eventType,
        String resultingStatus,
        String point,
        Long centerId,
        String cityName,
        Instant movedAt,
        Instant registeredAt,
        Instant occurredAt) implements DomainEvent {
}
