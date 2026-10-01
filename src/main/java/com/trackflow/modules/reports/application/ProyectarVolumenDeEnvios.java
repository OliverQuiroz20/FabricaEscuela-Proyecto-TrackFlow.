package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.ShipmentVolumeView;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantiene el modelo de lectura del reporte de volumen a partir de los eventos de
 * shipments y logistics. reports no puede importar el enum de logistics (regla del
 * monolito modular), así que reconoce los tipos de movimiento por el nombre que viaja
 * en el contrato de integración.
 */
@Service
public class ProyectarVolumenDeEnvios {

    static final String RECIBIDO_EN_CENTRO = "RECEIVED_AT_CENTER";
    static final String ENTREGADO = "DELIVERED";

    private final ShipmentVolumeViewRepository repository;

    public ProyectarVolumenDeEnvios(ShipmentVolumeViewRepository repository) {
        this.repository = repository;
    }

    /**
     * Si el envío ya está proyectado no se toca: la reconstrucción republica el alta de
     * todos los envíos, y sobrescribir la fila borraría el ingreso y la entrega ya
     * aplicados. La fecha de registro de un envío no cambia, así que no hay nada que
     * actualizar.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCrearEnvio(EnvioCreadoEvent event) {
        if (repository.findByTrackingNumber(event.trackingNumber()).isPresent()) {
            return;
        }

        repository.save(new ShipmentVolumeView(
                event.trackingNumber(),
                event.registeredAt(),
                event.originCityId(),
                event.originCity()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alRegistrarEvento(EventoLogisticoRegistradoEvent event) {
        boolean esIngreso = RECIBIDO_EN_CENTRO.equals(event.eventType());
        boolean esEntrega = ENTREGADO.equals(event.eventType());
        if (!esIngreso && !esEntrega) {
            return;
        }

        repository.findByTrackingNumber(event.trackingNumber())
                .ifPresent(view -> {
                    boolean aplicado = esIngreso
                            ? view.registrarIngreso(new ShipmentVolumeView.Ingreso(
                                    event.centerId(), event.point(), event.cityName(), event.movedAt()))
                            : view.registrarEntrega(event.movedAt());

                    if (aplicado) {
                        repository.save(view);
                    }
                });
    }
}
