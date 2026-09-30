package com.trackflow.modules.shipments.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Deja rastro de las solicitudes de envío que se rechazaron.
 */
@Component
public class SolicitudesDescartadasListener {

    private static final Logger log = LoggerFactory.getLogger(SolicitudesDescartadasListener.class);

    @RabbitListener(queues = ShipmentsRabbitMQConfig.DEAD_LETTER_QUEUE)
    public void recibir(EnvioSolicitadoMensaje mensaje) {
        log.error("Solicitud de envío descartada: eventId={} trackingNumber={} recipient={} requestedAt={}",
                mensaje.eventId(), mensaje.trackingNumber(),
                mensaje.recipient() != null ? mensaje.recipient().fullName() : null,
                mensaje.requestedAt());
    }
}
