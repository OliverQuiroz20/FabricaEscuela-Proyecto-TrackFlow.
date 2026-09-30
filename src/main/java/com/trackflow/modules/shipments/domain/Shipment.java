package com.trackflow.modules.shipments.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private TrackingNumber trackingNumber;

    @Embedded
    @AttributeOverride(name = "fullName", column = @Column(name = "sender_full_name", nullable = false))
    @AttributeOverride(name = "documentType", column = @Column(name = "sender_document_type", nullable = false, length = 5))
    @AttributeOverride(name = "documentNumber", column = @Column(name = "sender_document_number", nullable = false))
    @AttributeOverride(name = "phone", column = @Column(name = "sender_phone", nullable = false))
    @AttributeOverride(name = "address", column = @Column(name = "sender_address", nullable = false))
    @AttributeOverride(name = "cityId", column = @Column(name = "sender_city_id", nullable = false))
    private Party sender;

    @Embedded
    @AttributeOverride(name = "fullName", column = @Column(name = "recipient_full_name", nullable = false))
    @AttributeOverride(name = "documentType", column = @Column(name = "recipient_document_type", nullable = false, length = 5))
    @AttributeOverride(name = "documentNumber", column = @Column(name = "recipient_document_number", nullable = false))
    @AttributeOverride(name = "phone", column = @Column(name = "recipient_phone", nullable = false))
    @AttributeOverride(name = "address", column = @Column(name = "recipient_address", nullable = false))
    @AttributeOverride(name = "cityId", column = @Column(name = "recipient_city_id", nullable = false))
    private Party recipient;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    private String lastMovementPoint;

    private Instant lastMovementAt;

    protected Shipment() {
    }

    private Shipment(TrackingNumber trackingNumber, Party sender, Party recipient, String description,
            Instant registeredAt) {
        this.trackingNumber = trackingNumber;
        this.sender = sender;
        this.recipient = recipient;
        this.description = description;
        this.registeredAt = registeredAt;
        this.status = ShipmentStatus.REGISTERED;
    }

    public static Shipment registrar(TrackingNumber trackingNumber, Party sender, Party recipient, String description,
            Instant registeredAt) {
        return new Shipment(trackingNumber, sender, recipient, description, registeredAt);
    }

    /**
     * Aplica un movimiento siempre que sea posterior al último conocido.
     *
     * Los movimientos no llegan necesariamente en orden: el lector de una bodega sin
     * señal descarga sus registros horas después, y el broker puede reentregar un
     * mensaje. Sin esta comprobación, un movimiento reportado tarde devolvería el
     * envío a "en tránsito" cuando ya estaba entregado.
     *
     * @return false si el movimiento es anterior al último aplicado y se ignora
     */
    public boolean aplicarMovimiento(ShipmentStatus resultingStatus, String point, Instant movedAt) {
        if (lastMovementAt != null && movedAt.isBefore(lastMovementAt)) {
            return false;
        }

        this.status = resultingStatus;
        this.lastMovementPoint = point;
        this.lastMovementAt = movedAt;

        return true;
    }

    public Long getId() {
        return id;
    }

    public TrackingNumber getTrackingNumber() {
        return trackingNumber;
    }

    public Party getSender() {
        return sender;
    }

    public Party getRecipient() {
        return recipient;
    }

    public String getDescription() {
        return description;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public String getLastMovementPoint() {
        return lastMovementPoint;
    }

    public Instant getLastMovementAt() {
        return lastMovementAt;
    }
}
