package com.trackflow.modules.logistics.api.dto;

import com.trackflow.modules.logistics.domain.EventType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Instant;

public record RegistrarEventoRequest(
        @NotNull(message = "el tipo de evento es obligatorio") EventType type,

        /**
         * Id del centro del catálogo donde ocurrió el movimiento. Los válidos para
         * este envío en este momento vienen en
         * GET /api/shipments/{trackingNumber}/actions.
         *
         * Es obligatorio: mientras existió como respaldo un punto de texto libre, un
         * reporte sin centro se saltaba todas las reglas del recorrido, porque de un
         * texto no se puede saber en qué ciudad ocurrió.
         */
        @NotNull(message = "el centro es obligatorio") Long centerId,

        String notes,

        /**
         * Nombre de quien reparte. Obligatorio solo cuando {@code type} es
         * OUT_FOR_DELIVERY — para el resto de eventos no aplica y se ignora si llega.
         */
        String delivererName,

        /**
         * Cuándo ocurrió el movimiento. Opcional: si no se envía se asume que acaba de
         * ocurrir. Se reporta cuando el registro se sincroniza tarde, que es lo normal
         * si el lector de la bodega estuvo sin señal.
         */
        @PastOrPresent(message = "el movimiento no puede haber ocurrido en el futuro")
        Instant occurredAt) {

    @AssertTrue(message = "el repartidor es obligatorio para un evento OUT_FOR_DELIVERY")
    public boolean isDelivererPresentWhenRequired() {
        return type != EventType.OUT_FOR_DELIVERY || (delivererName != null && !delivererName.isBlank());
    }
}
