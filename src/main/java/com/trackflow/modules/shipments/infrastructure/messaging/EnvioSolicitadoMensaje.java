package com.trackflow.modules.shipments.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trackflow.modules.shipments.application.EnvioSolicitado;
import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;

/**
 * Contrato del mensaje que viaja por RabbitMQ, independiente del modelo de dominio.
 *
 * Es un lector tolerante: entiende los nombres en español de antes del contrato en
 * inglés (por si al desplegar quedan mensajes viejos en la cola) e ignora campos que
 * no conoce (por si un productor más nuevo agrega alguno), en vez de mandarlos a la DLQ.
 */
@JsonIgnoreProperties(value = "version", allowGetters = true, ignoreUnknown = true)
public record EnvioSolicitadoMensaje(
        String eventId,
        String trackingNumber,
        @JsonAlias("remitente") PersonaMensaje sender,
        @JsonAlias("destinatario") PersonaMensaje recipient,
        @JsonAlias("ciudadOrigen") CiudadMensaje originCity,
        @JsonAlias("ciudadDestino") CiudadMensaje destinationCity,
        @JsonAlias("descripcion") String description,
        @JsonAlias("solicitadoEn") Instant requestedAt) {

    /**
     * Versión del formato con que se produjo el mensaje. Viaja en cada mensaje para
     * que un consumidor futuro pueda distinguir formatos; al leer se ignora.
     */
    public static final int VERSION = 1;

    @JsonProperty("version")
    public int version() {
        return VERSION;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonaMensaje(
            @JsonAlias("nombreCompleto") String fullName,
            @JsonAlias("tipoDocumento") String documentType,
            @JsonAlias("numeroDocumento") String documentNumber,
            @JsonAlias("telefono") String phone,
            @JsonAlias("direccion") String address,
            @JsonAlias("ciudadId") Long cityId) {

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CiudadMensaje(Long id, @JsonAlias("nombre") String name,
            @JsonAlias("departamento") String department) {

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
