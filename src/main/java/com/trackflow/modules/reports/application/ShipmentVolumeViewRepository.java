package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.ShipmentVolumeView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Los rangos son semiabiertos, [desde, hasta): así dos periodos contiguos nunca cuentan
 * dos veces el envío registrado justo en la frontera.
 */
public interface ShipmentVolumeViewRepository {

    ShipmentVolumeView save(ShipmentVolumeView view);

    Optional<ShipmentVolumeView> findByTrackingNumber(String trackingNumber);

    /** Envíos registrados en el rango y ya recibidos en algún centro, agrupados por ese centro. */
    List<VolumenPorPunto> registradosPorPuntoDeIngreso(Instant desde, Instant hasta);

    /** Envíos registrados en el rango que aún no se reciben en ningún centro, por ciudad de origen. */
    List<VolumenPorPunto> registradosPendientesDeRecepcion(Instant desde, Instant hasta);

    long entregadosEntre(Instant desde, Instant hasta);
}
