package com.trackflow.modules.reports.api;

import com.trackflow.modules.reports.api.dto.VolumenDeEnviosResponse;
import com.trackflow.modules.reports.application.ConsultarVolumenDeEnvios;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reporte de volumen para el supervisor de operaciones: cuántos envíos se registraron
 * en un periodo y por qué punto de la red entraron, para dimensionar cada centro.
 *
 * Un periodo sin envíos responde 200 con total cero y un mensaje, no 404: el reporte
 * existe y su resultado es "no hubo envíos", que es información útil para quien
 * dimensiona la operación.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteVolumenController {

    private final ConsultarVolumenDeEnvios consultarVolumen;

    public ReporteVolumenController(ConsultarVolumenDeEnvios consultarVolumen) {
        this.consultarVolumen = consultarVolumen;
    }

    /**
     * @param desde primer día del periodo (AAAA-MM-DD, hora de Colombia); por defecto, hoy
     * @param hasta último día del periodo, inclusive; por defecto, el mismo día que desde
     */
    @GetMapping("/volumen-envios")
    public VolumenDeEnviosResponse volumen(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {
        return VolumenDeEnviosResponse.from(consultarVolumen.ejecutar(desde, hasta));
    }
}
