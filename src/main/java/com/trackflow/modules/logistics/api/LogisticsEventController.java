package com.trackflow.modules.logistics.api;

import com.trackflow.modules.logistics.api.dto.AccionesDisponiblesResponse;
import com.trackflow.modules.logistics.api.dto.EventoAdmitidoResponse;
import com.trackflow.modules.logistics.api.dto.HistorialResponse;
import com.trackflow.modules.logistics.api.dto.RegistrarEventoRequest;
import com.trackflow.modules.logistics.application.AdmitirEventoLogistico;
import com.trackflow.modules.logistics.application.ConsultarAccionesDisponibles;
import com.trackflow.modules.logistics.application.ConsultarHistorial;
import com.trackflow.modules.logistics.application.EventoLogisticoEntrante;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST. No registra el evento: lo admite y lo publica en el broker,
 * igual que haría cualquier punto de la cadena. Quien registra es el consumidor de la cola.
 */
@RestController
@RequestMapping("/api/shipments/{trackingNumber}")
public class LogisticsEventController {

    private final AdmitirEventoLogistico admitirEventoLogistico;
    private final ConsultarHistorial consultarHistorial;
    private final ConsultarAccionesDisponibles consultarAccionesDisponibles;

    public LogisticsEventController(AdmitirEventoLogistico admitirEventoLogistico,
            ConsultarHistorial consultarHistorial, ConsultarAccionesDisponibles consultarAccionesDisponibles) {
        this.admitirEventoLogistico = admitirEventoLogistico;
        this.consultarHistorial = consultarHistorial;
        this.consultarAccionesDisponibles = consultarAccionesDisponibles;
    }

    @PostMapping("/events")
    public ResponseEntity<EventoAdmitidoResponse> admitir(@PathVariable String trackingNumber,
            @Valid @RequestBody RegistrarEventoRequest request) {
        EventoLogisticoEntrante evento = admitirEventoLogistico.ejecutar(new AdmitirEventoLogistico.Command(
                trackingNumber,
                request.tipo(),
                request.centroId(),
                request.observaciones(),
                request.repartidorNombre(),
                request.ocurridoEn()));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(EventoAdmitidoResponse.from(evento));
    }

    /**
     * Historial del envío (HU-04). Público, como la consulta de estado: el número de
     * seguimiento es la credencial del cliente.
     */
    @GetMapping("/events")
    public HistorialResponse historial(@PathVariable String trackingNumber) {
        return HistorialResponse.from(trackingNumber, consultarHistorial.ejecutar(trackingNumber));
    }

    /**
     * Qué movimientos admite el envío ahora y con qué centros, para que el operador
     * escoja de una lista corta y correcta en vez del catálogo entero.
     */
    @GetMapping("/acciones")
    public AccionesDisponiblesResponse acciones(@PathVariable String trackingNumber) {
        return AccionesDisponiblesResponse.from(consultarAccionesDisponibles.ejecutar(trackingNumber));
    }
}
