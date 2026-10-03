package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.ProyectarVolumenDeEnvios;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Alimenta la proyección del reporte de volumen con los eventos de shipments y
 * logistics. Va aparte de SeguimientoProjectionSubscriber para que un fallo en una
 * proyección no impida actualizar la otra: cada una corre en su propia transacción.
 */
@Component
public class VolumenProjectionSubscriber {

    private final ProyectarVolumenDeEnvios proyeccion;

    public VolumenProjectionSubscriber(ProyectarVolumenDeEnvios proyeccion) {
        this.proyeccion = proyeccion;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EnvioCreadoEvent event) {
        proyeccion.alCrearEnvio(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EventoLogisticoRegistradoEvent event) {
        proyeccion.alRegistrarEvento(event);
    }
}
