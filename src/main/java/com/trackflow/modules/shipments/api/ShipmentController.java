package com.trackflow.modules.shipments.api;

import com.trackflow.modules.shipments.api.dto.EnvioAdmitidoResponse;
import com.trackflow.modules.shipments.api.dto.RegistrarEnvioRequest;
import com.trackflow.modules.shipments.application.AdmitirEnvio;
import com.trackflow.modules.shipments.application.EnvioSolicitado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST. Valida y publica la solicitud en el broker; el envío
 * lo registra el consumidor de la cola. El número de seguimiento se devuelve ya,
 * porque HU-01 exige entregárselo al remitente en el momento.
 */
@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final AdmitirEnvio admitirEnvio;

    public ShipmentController(AdmitirEnvio admitirEnvio) {
        this.admitirEnvio = admitirEnvio;
    }

    @PostMapping
    public ResponseEntity<EnvioAdmitidoResponse> admitir(@Valid @RequestBody RegistrarEnvioRequest request) {
        EnvioSolicitado solicitud = admitirEnvio.ejecutar(new AdmitirEnvio.Command(
                request.sender().toDatos(),
                request.recipient().toDatos(),
                request.description()));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(EnvioAdmitidoResponse.from(solicitud));
    }
}
