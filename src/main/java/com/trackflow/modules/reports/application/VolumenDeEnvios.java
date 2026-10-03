package com.trackflow.modules.reports.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado del reporte de volumen para un periodo.
 *
 * @param desde             primer día del periodo (inclusive), en hora de Colombia
 * @param hasta             último día del periodo (inclusive), en hora de Colombia
 * @param inicio            instante desde el que se cuenta
 * @param corte             instante hasta el que se cuenta (exclusivo); si el periodo
 *                          incluye el día en curso, es el momento de la consulta
 * @param enCurso           true si el periodo incluye el día en curso y por tanto aún
 *                          puede crecer
 * @param totalRegistrados  envíos registrados en el periodo
 * @param totalEntregados   envíos entregados en el periodo, sin importar cuándo se
 *                          registraron; es una cifra aparte, no un subconjunto del total
 * @param porPunto          desglose de {@code totalRegistrados} por punto de ingreso a la
 *                          red; siempre suma el total
 */
public record VolumenDeEnvios(
        LocalDate desde,
        LocalDate hasta,
        Instant inicio,
        Instant corte,
        boolean enCurso,
        long totalRegistrados,
        long totalEntregados,
        List<VolumenPorPunto> porPunto) {

    public boolean hayEnvios() {
        return totalRegistrados > 0;
    }
}
