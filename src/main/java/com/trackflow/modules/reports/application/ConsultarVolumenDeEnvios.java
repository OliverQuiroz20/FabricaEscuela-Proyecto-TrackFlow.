package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.PeriodoInvalidoException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuántos envíos se registraron en un periodo y por qué punto de la red entraron.
 *
 * El periodo se expresa en días calendario de Colombia, que es como piensa la
 * operación: "el 15 de septiembre" va de la medianoche a la medianoche de Bogotá, no
 * de UTC (que lo correría cinco horas). Sin fechas, el periodo es el día en curso, y
 * cuando el periodo incluye hoy se cuenta solo hasta el momento de la consulta.
 */
@Service
public class ConsultarVolumenDeEnvios {

    static final ZoneId ZONA_OPERACION = ZoneId.of("America/Bogota");

    /** Los puntos con más carga primero; los pendientes de recepción al final. */
    private static final Comparator<VolumenPorPunto> ORDEN = Comparator
            .comparing(VolumenPorPunto::pendienteDeRecepcion)
            .thenComparing(VolumenPorPunto::envios, Comparator.reverseOrder())
            .thenComparing(VolumenPorPunto::punto, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ShipmentVolumeViewRepository repository;
    private final Clock clock;

    public ConsultarVolumenDeEnvios(ShipmentVolumeViewRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * @param desde primer día, inclusive; si es null, hoy
     * @param hasta último día, inclusive; si es null, el mismo día que {@code desde}
     */
    @Transactional(readOnly = true)
    public VolumenDeEnvios ejecutar(LocalDate desde, LocalDate hasta) {
        Instant ahora = clock.instant();
        LocalDate hoy = LocalDate.ofInstant(ahora, ZONA_OPERACION);

        LocalDate primerDia = desde == null ? hoy : desde;
        LocalDate ultimoDia = hasta == null ? primerDia : hasta;
        validar(primerDia, ultimoDia, hoy);

        Instant inicio = primerDia.atStartOfDay(ZONA_OPERACION).toInstant();
        Instant finDelPeriodo = ultimoDia.plusDays(1).atStartOfDay(ZONA_OPERACION).toInstant();
        boolean enCurso = !ultimoDia.isBefore(hoy);
        Instant corte = enCurso ? ahora : finDelPeriodo;

        List<VolumenPorPunto> porPunto = new ArrayList<>(repository.registradosPorPuntoDeIngreso(inicio, corte));
        porPunto.addAll(repository.registradosPendientesDeRecepcion(inicio, corte));
        porPunto.sort(ORDEN);

        long totalRegistrados = porPunto.stream().mapToLong(VolumenPorPunto::envios).sum();
        long totalEntregados = repository.entregadosEntre(inicio, corte);

        return new VolumenDeEnvios(primerDia, ultimoDia, inicio, corte, enCurso,
                totalRegistrados, totalEntregados, List.copyOf(porPunto));
    }

    private static void validar(LocalDate primerDia, LocalDate ultimoDia, LocalDate hoy) {
        if (ultimoDia.isBefore(primerDia)) {
            throw new PeriodoInvalidoException(
                    "El periodo está invertido: 'hasta' (%s) es anterior a 'desde' (%s)".formatted(ultimoDia, primerDia));
        }
        if (primerDia.isAfter(hoy)) {
            throw new PeriodoInvalidoException(
                    "El periodo empieza en el futuro (%s); hoy es %s y el reporte solo cubre envíos ya registrados"
                            .formatted(primerDia, hoy));
        }
    }
}
