package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.domain.ShipmentVolumeView;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentVolumeViewJpaRepository extends JpaRepository<ShipmentVolumeView, String> {

    /** Fila agregada: un punto y cuántos envíos le corresponden. */
    interface PuntoAgrupado {
        Long getCentroId();

        String getPunto();

        String getCiudad();

        Long getEnvios();
    }

    /** Fila agregada: una ciudad de origen y cuántos envíos esperan ingresar por ella. */
    interface CiudadAgrupada {
        String getCiudad();

        Long getEnvios();
    }

    @Query("""
            select v.entryCenterId as centroId, v.entryPoint as punto, v.entryCity as ciudad, count(v) as envios
            from ShipmentVolumeView v
            where v.registeredAt >= :desde and v.registeredAt < :hasta
              and v.receivedAt is not null
            group by v.entryCenterId, v.entryPoint, v.entryCity
            """)
    List<PuntoAgrupado> contarPorPuntoDeIngreso(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            select v.originCity as ciudad, count(v) as envios
            from ShipmentVolumeView v
            where v.registeredAt >= :desde and v.registeredAt < :hasta
              and v.receivedAt is null
            group by v.originCityId, v.originCity
            """)
    List<CiudadAgrupada> contarPendientesPorCiudadDeOrigen(@Param("desde") Instant desde,
            @Param("hasta") Instant hasta);

    @Query("""
            select count(v) from ShipmentVolumeView v
            where v.deliveredAt >= :desde and v.deliveredAt < :hasta
            """)
    long contarEntregados(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
