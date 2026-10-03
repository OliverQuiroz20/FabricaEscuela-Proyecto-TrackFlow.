package com.trackflow.bootstrap;

import com.trackflow.modules.logistics.application.RepublicarEventosLogisticos;
import com.trackflow.modules.shipments.application.RepublicarEnviosCreados;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reconstruye las proyecciones a partir de la fuente de verdad, para cuando una
 * quede desincronizada porque su actualización falló tras registrarse el evento.
 *
 * Vive en bootstrap porque coordina dos módulos: es el único punto que puede
 * conocerlos a ambos sin acoplarlos entre sí.
 */
@RestController
@RequestMapping("/api/admin")
public class ReconstruccionController {

    private final RepublicarEnviosCreados envios;
    private final RepublicarEventosLogisticos eventos;

    public ReconstruccionController(RepublicarEnviosCreados envios, RepublicarEventosLogisticos eventos) {
        this.envios = envios;
        this.eventos = eventos;
    }

    /**
     * El orden importa: primero los envíos, para que existan antes de reaplicarles
     * sus movimientos; después los eventos, en el orden en que ocurrieron.
     */
    @PostMapping("/rebuild-projections")
    public Map<String, Integer> reconstruir() {
        int enviosRepublicados = envios.ejecutar();
        int eventosRepublicados = eventos.ejecutar();

        return Map.of(
                "shipmentsRepublished", enviosRepublicados,
                "eventsRepublished", eventosRepublicados);
    }
}
