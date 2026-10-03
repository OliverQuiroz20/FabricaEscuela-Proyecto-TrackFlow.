package com.trackflow.modules.shipments.infrastructure.messaging;

import com.trackflow.modules.shipments.application.EnvioSolicitado;
import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;

/**
 * Contrato del mensaje que viaja por RabbitMQ, independiente del modelo de dominio.
 */
public record EnvioSolicitadoMensaje(
        String eventId,
        String trackingNumber,
        PersonaMensaje sender,
        PersonaMensaje recipient,
        CiudadMensaje originCity,
        CiudadMensaje destinationCity,
        String description,
        Instant requestedAt) {

    public record PersonaMensaje(
            String fullName,
            String documentType,
            String documentNumber,
            String phone,
            String address,
            Long cityId) {

        static PersonaMensaje from(Party party) {
            return new PersonaMensaje(
                    party.getFullName(),
                    party.getDocumentType().name(),
                    party.getDocumentNumber(),
                    party.getPhone(),
                    party.getAddress(),
                    party.getCityId());
        }

        Party toDomain() {
            return new Party(fullName, TipoDocumento.valueOf(documentType), documentNumber,
                    phone, address, cityId);
        }
    }

    public record CiudadMensaje(Long id, String name, String department) {

        static CiudadMensaje from(Ciudad ciudad) {
            return new CiudadMensaje(ciudad.id(), ciudad.nombre(), ciudad.departamento());
        }

        Ciudad toDomain() {
            return new Ciudad(id, name, department);
        }
    }

    public static EnvioSolicitadoMensaje from(EnvioSolicitado solicitud) {
        return new EnvioSolicitadoMensaje(
                solicitud.eventId(),
                solicitud.trackingNumber(),
                PersonaMensaje.from(solicitud.remitente()),
                PersonaMensaje.from(solicitud.destinatario()),
                CiudadMensaje.from(solicitud.ciudadOrigen()),
                CiudadMensaje.from(solicitud.ciudadDestino()),
                solicitud.descripcion(),
                solicitud.solicitadoEn());
    }

    public EnvioSolicitado toSolicitud() {
        return new EnvioSolicitado(
                eventId,
                trackingNumber,
                sender.toDomain(),
                recipient.toDomain(),
                originCity.toDomain(),
                destinationCity.toDomain(),
                description,
                requestedAt);
    }
}
