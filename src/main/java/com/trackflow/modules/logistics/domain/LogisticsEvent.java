package com.trackflow.modules.logistics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "logistics_events")
public class LogisticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del mensaje de ingesta. Unico para descartar reentregas del broker. */
    @Column(nullable = false, unique = true, updatable = false)
    private String eventId;

    @Column(nullable = false, updatable = false)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private EventType type;

    @Column(nullable = false, updatable = false)
    private String point;

    private String notes;

    /**
     * Id del centro del catálogo, si el evento se reportó con uno. Nullable a
     * propósito: eventos históricos y los que usan el respaldo de texto libre no
     * tienen centro asociado. No se resuelve como referencia viva — si el centro se
     * renombra o desactiva después, este evento no cambia.
     */
    @Column(updatable = false)
    private Long centerId;

    /** Nombre de la ciudad del centro, congelado en el momento del registro. */
    @Column(updatable = false)
    private String cityName;

    /** Quien reparte. Solo tiene valor en eventos OUT_FOR_DELIVERY. */
    @Column(updatable = false)
    private String delivererName;

    /** Cuándo ocurrió el movimiento. Es la fecha que ve el cliente y la que ordena el historial. */
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    /** Cuándo se recibió el reporte. Sirve para auditar cuánto tardó en reportarse. */
    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    /**
     * Dónde ocurrió el movimiento.
     *
     * @param centerId id del centro del catálogo, o null si se reportó con texto libre
     * @param cityName ciudad del centro, congelada en el momento del registro
     */
    public record Lugar(String point, Long centerId, String cityName) {
    }

    /** Lo que reporta el operador, antes de que el sistema le ponga fecha de registro. */
    public record Reporte(String eventId, String trackingNumber, EventType type, Lugar lugar, String notes,
            String delivererName, Instant occurredAt) {
    }

    protected LogisticsEvent() {
    }

    private LogisticsEvent(Reporte reporte, Instant registeredAt) {
        this.eventId = reporte.eventId();
        this.trackingNumber = reporte.trackingNumber();
        this.type = reporte.type();
        this.point = reporte.lugar().point();
        this.notes = reporte.notes();
        this.centerId = reporte.lugar().centerId();
        this.cityName = reporte.lugar().cityName();
        this.delivererName = reporte.delivererName();
        this.occurredAt = reporte.occurredAt();
        this.registeredAt = registeredAt;
    }

    /**
     * Un movimiento no puede haber ocurrido después de reportarse. Se comprueba aquí
     * y no en el DTO porque el reporte llega por REST y también por la cola.
     */
    public static LogisticsEvent registrar(Reporte reporte, Instant registeredAt) {
        Instant occurredAt = reporte.occurredAt();
        if (occurredAt == null || occurredAt.isAfter(registeredAt)) {
            throw new FechaDeMovimientoInvalidaException(occurredAt, registeredAt);
        }

        return new LogisticsEvent(reporte, registeredAt);
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public EventType getType() {
        return type;
    }

    public String getPoint() {
        return point;
    }

    public String getNotes() {
        return notes;
    }

    public Long getCenterId() {
        return centerId;
    }

    public String getCityName() {
        return cityName;
    }

    public String getDelivererName() {
        return delivererName;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
