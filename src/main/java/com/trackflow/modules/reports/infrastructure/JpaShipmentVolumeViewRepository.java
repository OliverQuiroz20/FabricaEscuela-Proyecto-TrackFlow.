package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.ShipmentVolumeViewRepository;
import com.trackflow.modules.reports.application.VolumenPorPunto;
import com.trackflow.modules.reports.domain.ShipmentVolumeView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaShipmentVolumeViewRepository implements ShipmentVolumeViewRepository {

    /** Etiqueta de un ingreso histórico registrado con texto libre y sin nombre de punto. */
    private static final String PUNTO_SIN_NOMBRE = "Punto sin identificar";

    /** Los envíos que aún no se reciben en ningún centro se esperan en su ciudad de origen. */
    private static final String PENDIENTE_DE_RECEPCION = "Pendiente de recepción en ";

    private final ShipmentVolumeViewJpaRepository jpa;

    public JpaShipmentVolumeViewRepository(ShipmentVolumeViewJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ShipmentVolumeView save(ShipmentVolumeView view) {
        return jpa.save(view);
    }

    @Override
    public Optional<ShipmentVolumeView> findByTrackingNumber(String trackingNumber) {
        return jpa.findById(trackingNumber);
    }

    @Override
    public List<VolumenPorPunto> registradosPorPuntoDeIngreso(Instant desde, Instant hasta) {
        return jpa.contarPorPuntoDeIngreso(desde, hasta).stream()
                .map(fila -> new VolumenPorPunto(
                        fila.getCentroId(),
                        fila.getPunto() == null ? PUNTO_SIN_NOMBRE : fila.getPunto(),
                        fila.getCiudad(),
                        false,
                        fila.getEnvios()))
                .toList();
    }

    @Override
    public List<VolumenPorPunto> registradosPendientesDeRecepcion(Instant desde, Instant hasta) {
        return jpa.contarPendientesPorCiudadDeOrigen(desde, hasta).stream()
                .map(fila -> new VolumenPorPunto(
                        null,
                        PENDIENTE_DE_RECEPCION + fila.getCiudad(),
                        fila.getCiudad(),
                        true,
                        fila.getEnvios()))
                .toList();
    }

    @Override
    public long entregadosEntre(Instant desde, Instant hasta) {
        return jpa.contarEntregados(desde, hasta);
    }
}
