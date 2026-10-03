package com.trackflow.modules.shipments.application;

import com.trackflow.modules.shipments.domain.Shipment;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarEnvio {

    private static final Logger log = LoggerFactory.getLogger(RegistrarEnvio.class);

    private final ShipmentRepository shipments;
    private final EventPublisher events;
    private final Clock clock;

    public RegistrarEnvio(ShipmentRepository shipments, EventPublisher events, Clock clock) {
        this.shipments = shipments;
        this.events = events;
        this.clock = clock;
    }

    /**
     * El broker entrega at-least-once: si la solicitud llega repetida, el número de
     * seguimiento ya existe y se descarta en lugar de registrar el envío dos veces.
     */
    @Transactional
    public Optional<Shipment> ejecutar(EnvioSolicitado solicitud) {
        TrackingNumber trackingNumber = TrackingNumber.of(solicitud.trackingNumber());

        if (shipments.existsByTrackingNumber(trackingNumber)) {
            log.info("Envío {} ya registrado, se descarta la reentrega", solicitud.trackingNumber());
            return Optional.empty();
        }

        Instant now = clock.instant();
        // El envío existe desde que se admitió, no desde que se consumió la cola: si el
        // consumo se atrasa o se reprocesa, la fecha de registro no debe moverse, ni
        // quedar posterior a los movimientos que ya tiene. Los mensajes sin la fecha
        // (anteriores a que viajara en el mensaje) caen en la hora de consumo.
        Instant registradoEn = solicitud.solicitadoEn() != null ? solicitud.solicitadoEn() : now;
        Shipment saved = shipments.save(Shipment.registrar(
                trackingNumber,
                solicitud.remitente(),
                solicitud.destinatario(),
                solicitud.descripcion(),
                registradoEn));

        events.publish(new EnvioCreadoEvent(
                saved.getTrackingNumber().value(),
                saved.getStatus().name(),
                saved.getSender().getFullName(),
                solicitud.ciudadOrigen().id(),
                solicitud.ciudadOrigen().etiqueta(),
                saved.getRecipient().getFullName(),
                saved.getRecipient().getAddress(),
                solicitud.ciudadDestino().id(),
                solicitud.ciudadDestino().etiqueta(),
                saved.getRegisteredAt(),
                now));

        return Optional.of(saved);
    }
}
