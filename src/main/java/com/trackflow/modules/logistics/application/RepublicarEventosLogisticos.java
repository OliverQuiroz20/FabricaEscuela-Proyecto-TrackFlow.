package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.shared.events.EventPublisher;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vuelve a publicar el historial completo en orden cronológico. Al reaplicarse en
 * el mismo orden en que ocurrieron, las proyecciones quedan en el estado correcto.
 */
@Service
public class RepublicarEventosLogisticos {

    private static final Logger log = LoggerFactory.getLogger(RepublicarEventosLogisticos.class);

    private final LogisticsEventRepository logisticsEvents;
    private final EventPublisher events;
    private final Clock clock;

    public RepublicarEventosLogisticos(LogisticsEventRepository logisticsEvents, EventPublisher events, Clock clock) {
        this.logisticsEvents = logisticsEvents;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public int ejecutar() {
        List<LogisticsEvent> todos = logisticsEvents.findTodosCronologicamente();

        for (LogisticsEvent evento : todos) {
            events.publish(new EventoLogisticoRegistradoEvent(
                    evento.getId(),
                    evento.getTrackingNumber(),
                    evento.getType().name(),
                    evento.getType().resultingStatus(),
                    evento.getPoint(),
                    evento.getCenterId(),
                    evento.getCityName(),
                    evento.getOccurredAt(),
                    evento.getRegisteredAt(),
                    clock.instant()));
        }

        log.info("Republicados {} eventos logísticos para reconstrucción", todos.size());
        return todos.size();
    }
}
