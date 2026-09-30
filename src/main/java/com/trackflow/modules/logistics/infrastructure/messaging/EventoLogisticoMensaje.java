package com.trackflow.modules.logistics.infrastructure.messaging;

import com.trackflow.modules.logistics.application.EventoLogisticoEntrante;
import com.trackflow.modules.logistics.domain.EventType;
import java.time.Instant;

/**
 * Contrato del mensaje que viaja por RabbitMQ. Es la forma pública que consumen
 * los puntos de la cadena, separada del modelo interno de la aplicación.
 */
public record EventoLogisticoMensaje(
        String eventId,
        String trackingNumber,
        String type,
        Long centerId,
        String point,
        String cityName,
        String notes,
        String delivererName,
        Instant occurredAt) {

    public static EventoLogisticoMensaje from(EventoLogisticoEntrante evento) {
        return new EventoLogisticoMensaje(
                evento.eventId(),
                evento.trackingNumber(),
                evento.tipo().name(),
                evento.centroId(),
                evento.punto(),
                evento.ciudadNombre(),
                evento.observaciones(),
                evento.repartidorNombre(),
                evento.ocurridoEn());
    }

    public EventoLogisticoEntrante toEntrante() {
        return new EventoLogisticoEntrante(
                eventId,
                trackingNumber,
                EventType.valueOf(type),
                centerId,
                point,
                cityName,
                notes,
                delivererName,
                occurredAt);
    }
}
