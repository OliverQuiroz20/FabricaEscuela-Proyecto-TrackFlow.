package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyectarSeguimientoEnvio {

    private final ShipmentTrackingViewRepository repository;

    public ProyectarSeguimientoEnvio(ShipmentTrackingViewRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCrearEnvio(EnvioCreadoEvent event) {
        ShipmentTrackingView view = new ShipmentTrackingView(
                event.trackingNumber(),
                event.status(),
                new ShipmentTrackingView.Origen(event.senderName(), event.originCityId(), event.originCity()),
                new ShipmentTrackingView.Destino(event.recipientName(), event.recipientAddress(),
                        event.destinationCityId(), event.destinationCity()),
                event.registeredAt()
        );
        repository.save(view);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alRegistrarEvento(EventoLogisticoRegistradoEvent event) {
        repository.findByTrackingNumber(event.trackingNumber())
                .ifPresent(view -> {
                    boolean aplicado = view.registrarMovimiento(
                            event.resultingStatus(),
                            event.point(),
                            event.movedAt()
                    );

                    if (aplicado) {
                        repository.save(view);
                    }
                });
    }
}