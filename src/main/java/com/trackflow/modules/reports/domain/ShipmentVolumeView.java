package com.trackflow.modules.reports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Modelo de lectura del reporte de volumen: una fila por envío con lo necesario para
 * contarlo por periodo y por punto de la red, sin datos personales.
 *
 * El punto de la red es el de ingreso: el centro del primer "Recibido en centro". Hasta
 * que eso ocurre el envío no ha entrado a ningún centro y el reporte lo agrupa como
 * pendiente de recepción en su ciudad de origen, de modo que el desglose suma el total.
 */
@Entity
@Table(name = "reports_shipment_volume")
public class ShipmentVolumeView {

    @Id
    private String trackingNumber;

    @Column(nullable = false)
    private Instant registeredAt;

    @Column(nullable = false)
    private Long originCityId;

    @Column(nullable = false)
    private String originCity;

    /** Centro del catálogo por el que entró. Null si aún no entra o si se reportó con texto libre. */
    @Column
    private Long entryCenterId;

    /** Nombre del punto de ingreso, congelado en el momento del movimiento. */
    @Column
    private String entryPoint;

    /** Ciudad del punto de ingreso, congelada en el momento del movimiento. */
    @Column
    private String entryCity;

    /** Cuándo se recibió por primera vez en un centro. */
    @Column
    private Instant receivedAt;

    /** Cuándo se entregó al destinatario. */
    @Column
    private Instant deliveredAt;

    /** Dónde entró el envío a la red y cuándo. */
    public record Ingreso(Long centerId, String point, String city, Instant at) {
    }

    protected ShipmentVolumeView() {
    }

    public ShipmentVolumeView(String trackingNumber, Instant registeredAt, Long originCityId, String originCity) {
        this.trackingNumber = trackingNumber;
        this.registeredAt = registeredAt;
        this.originCityId = originCityId;
        this.originCity = originCity;
    }

    /**
     * Se queda con el ingreso más temprano: los reportes pueden llegar fuera de orden
     * (un lector sin señal sincroniza tarde, o el broker reentrega un mensaje), y el
     * punto de ingreso es por definición el primero, no el último que llegó.
     *
     * @return false si ya había un ingreso anterior o igual y este se ignora
     */
    public boolean registrarIngreso(Ingreso ingreso) {
        if (receivedAt != null && !ingreso.at().isBefore(receivedAt)) {
            return false;
        }

        this.entryCenterId = ingreso.centerId();
        this.entryPoint = ingreso.point();
        this.entryCity = ingreso.city();
        this.receivedAt = ingreso.at();

        return true;
    }

    /**
     * Misma regla que el ingreso: cuenta la primera entrega, y una reentrega del
     * mensaje no la mueve de periodo.
     *
     * @return false si ya había una entrega anterior o igual y esta se ignora
     */
    public boolean registrarEntrega(Instant at) {
        if (deliveredAt != null && !at.isBefore(deliveredAt)) {
            return false;
        }

        this.deliveredAt = at;
        return true;
    }

    public boolean ingresoALaRed() {
        return receivedAt != null;
    }

    public String getTrackingNumber() { return trackingNumber; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Long getOriginCityId() { return originCityId; }
    public String getOriginCity() { return originCity; }
    public Long getEntryCenterId() { return entryCenterId; }
    public String getEntryPoint() { return entryPoint; }
    public String getEntryCity() { return entryCity; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
}
