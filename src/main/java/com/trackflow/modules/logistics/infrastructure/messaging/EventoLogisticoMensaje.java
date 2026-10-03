package com.trackflow.modules.logistics.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trackflow.modules.logistics.application.EventoLogisticoEntrante;
import com.trackflow.modules.logistics.domain.EventType;
import java.time.Instant;

/**
 * Contrato del mensaje que viaja por RabbitMQ. Es la forma pública que consumen
 * los puntos de la cadena, separada del modelo interno de la aplicación.
 *
 * Es un lector tolerante: entiende los nombres en español de antes del contrato en
 * inglés (por si al desplegar quedan mensajes viejos en la cola) e ignora campos que
 * no conoce (por si un productor más nuevo agrega alguno), en vez de mandarlos a la DLQ.
 */
@JsonIgnoreProperties(value = "version", allowGetters = true, ignoreUnknown = true)
public record EventoLogisticoMensaje(
        String eventId,
        String trackingNumber,
        @JsonAlias("tipo") String type,
        @JsonAlias("centroId") Long centerId,
        @JsonAlias("punto") String point,
        @JsonAlias("ciudadNombre") String cityName,
        @JsonAlias("observaciones") String notes,
        @JsonAlias("repartidorNombre") String delivererName,
        @JsonAlias("ocurridoEn") Instant occurredAt) {

    /**
     * Versión del formato con que se produjo el mensaje. Viaja en cada mensaje para
     * que un consumidor futuro pueda distinguir formatos; al leer se ignora.
     */
    public static final int VERSION = 1;

    @JsonProperty("version")
    public int version() {
        return VERSION;
    }

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
