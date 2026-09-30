package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.events.EventPublisher;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarEventoLogistico {

    private static final Logger log = LoggerFactory.getLogger(RegistrarEventoLogistico.class);

    private final LogisticsEventRepository logisticsEvents;
    private final TrackedShipmentRepository trackedShipments;
    private final EventPublisher events;
    private final Clock clock;

    public RegistrarEventoLogistico(LogisticsEventRepository logisticsEvents,
            TrackedShipmentRepository trackedShipments, EventPublisher events, Clock clock) {
        this.logisticsEvents = logisticsEvents;
        this.trackedShipments = trackedShipments;
        this.events = events;
        this.clock = clock;
    }

    /**
     * El broker entrega at-least-once, así que un mismo eventId puede llegar más de una vez;
     * la segunda se descarta en lugar de duplicar el movimiento en el historial.
     */
    @Transactional
    public Optional<LogisticsEvent> ejecutar(EventoLogisticoEntrante entrante) {
        if (logisticsEvents.existePorEventId(entrante.eventId())) {
            log.info("Evento {} ya registrado, se descarta la reentrega", entrante.eventId());
            return Optional.empty();
        }

        if (!trackedShipments.exists(entrante.trackingNumber())) {
            throw new UnknownShipmentException(entrante.trackingNumber());
        }

        Instant now = clock.instant();
        LogisticsEvent saved = logisticsEvents.save(LogisticsEvent.registrar(
                new LogisticsEvent.Reporte(
                        entrante.eventId(),
                        entrante.trackingNumber(),
                        entrante.tipo(),
                        new LogisticsEvent.Lugar(entrante.punto(), entrante.centroId(), entrante.ciudadNombre()),
                        entrante.observaciones(),
                        entrante.repartidorNombre(),
                        entrante.ocurridoEn()),
                now));

        events.publish(new EventoLogisticoRegistradoEvent(
                saved.getId(),
                saved.getTrackingNumber(),
                saved.getType().name(),
                saved.getType().resultingStatus(),
                saved.getPoint(),
                saved.getOccurredAt(),
                saved.getRegisteredAt(),
                now));

        return Optional.of(saved);
    }
}
