package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.ShipmentVolumeView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Doble de prueba con la misma semántica que las consultas JPQL: rangos [desde, hasta). */
class RepositorioDeVolumenEnMemoria implements ShipmentVolumeViewRepository {

    private final Map<String, ShipmentVolumeView> filas = new LinkedHashMap<>();

    @Override
    public ShipmentVolumeView save(ShipmentVolumeView view) {
        filas.put(view.getTrackingNumber(), view);
        return view;
    }

    @Override
    public Optional<ShipmentVolumeView> findByTrackingNumber(String trackingNumber) {
        return Optional.ofNullable(filas.get(trackingNumber));
    }

    @Override
    public List<VolumenPorPunto> registradosPorPuntoDeIngreso(Instant desde, Instant hasta) {
        record Clave(Long centroId, String punto, String ciudad) {
        }
        Map<Clave, Long> conteo = filas.values().stream()
                .filter(v -> enRango(v.getRegisteredAt(), desde, hasta) && v.ingresoALaRed())
                .collect(Collectors.groupingBy(v -> new Clave(v.getEntryCenterId(), v.getEntryPoint(), v.getEntryCity()),
                        LinkedHashMap::new, Collectors.counting()));

        List<VolumenPorPunto> resultado = new ArrayList<>();
        conteo.forEach((k, n) -> resultado.add(new VolumenPorPunto(k.centroId(), k.punto(), k.ciudad(), false, n)));
        return resultado;
    }

    @Override
    public List<VolumenPorPunto> registradosPendientesDeRecepcion(Instant desde, Instant hasta) {
        Map<String, Long> conteo = filas.values().stream()
                .filter(v -> enRango(v.getRegisteredAt(), desde, hasta) && !v.ingresoALaRed())
                .collect(Collectors.groupingBy(ShipmentVolumeView::getOriginCity, LinkedHashMap::new,
                        Collectors.counting()));

        List<VolumenPorPunto> resultado = new ArrayList<>();
        conteo.forEach((ciudad, n) -> resultado.add(
                new VolumenPorPunto(null, "Pendiente de recepción en " + ciudad, ciudad, true, n)));
        return resultado;
    }

    @Override
    public long entregadosEntre(Instant desde, Instant hasta) {
        return filas.values().stream()
                .map(ShipmentVolumeView::getDeliveredAt)
                .filter(Objects::nonNull)
                .filter(at -> enRango(at, desde, hasta))
                .count();
    }

    private static boolean enRango(Instant at, Instant desde, Instant hasta) {
        return !at.isBefore(desde) && at.isBefore(hasta);
    }
}
