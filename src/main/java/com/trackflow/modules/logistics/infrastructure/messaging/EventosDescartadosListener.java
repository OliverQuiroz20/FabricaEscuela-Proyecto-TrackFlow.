package com.trackflow.modules.logistics.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Deja rastro de los eventos que se rechazaron. Sin esto los mensajes caen en la
 * cola de descarte y nadie se entera de que se perdieron.
 */
@Component
public class EventosDescartadosListener {

    private static final Logger log = LoggerFactory.getLogger(EventosDescartadosListener.class);

    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    public void recibir(EventoLogisticoMensaje mensaje) {
        log.error("Evento logístico descartado: eventId={} trackingNumber={} type={} point={} occurredAt={}",
                mensaje.eventId(), mensaje.trackingNumber(), mensaje.type(), mensaje.point(), mensaje.occurredAt());
    }
}
