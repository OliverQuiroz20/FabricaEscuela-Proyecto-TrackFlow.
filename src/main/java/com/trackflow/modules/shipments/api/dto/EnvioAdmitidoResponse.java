package com.trackflow.modules.shipments.api.dto;

import com.trackflow.modules.shipments.application.EnvioSolicitado;
import java.time.Instant;

public record EnvioAdmitidoResponse(
        String trackingNumber,
        String eventId,
        String recipient,
        String destinationCity,
        Instant requestedAt,
        String processingStatus) {

    public static EnvioAdmitidoResponse from(EnvioSolicitado solicitud) {
        return new EnvioAdmitidoResponse(
                solicitud.trackingNumber(),
                solicitud.eventId(),
                solicitud.destinatario().getFullName(),
                solicitud.ciudadDestino().etiqueta(),
                solicitud.solicitadoEn(),
                "QUEUED");
    }
}
