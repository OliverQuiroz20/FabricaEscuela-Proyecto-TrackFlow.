package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CentroFueraDeCiudadException;
import com.trackflow.modules.logistics.domain.CiudadEsperada;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaEnUsoException;
import com.trackflow.modules.logistics.domain.ClaveDeIdempotenciaInvalidaException;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FechaDeMovimientoInvalidaException;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.modules.logistics.domain.LogisticsEvent;
import com.trackflow.modules.logistics.domain.MovimientoFueraDeOrdenException;
import com.trackflow.modules.logistics.domain.TrackedShipment;
import com.trackflow.modules.logistics.domain.TransicionInvalidaException;
import com.trackflow.modules.logistics.domain.UnknownShipmentException;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Admite un evento reportado por un punto de la cadena y lo encola.
 * No lo registra: de eso se encarga {@link RegistrarEventoLogistico} al consumirlo.
 *
 * Aquí se concentran todas las reglas del recorrido, antes de encolar: un mensaje que
 * no tiene sentido se rechaza con su código HTTP en vez de descartarse en silencio en
 * la cola de reintentos.
 */
@Service
public class AdmitirEventoLogistico {

    /**
     * {@code centroId} identifica el centro del catálogo donde ocurrió el movimiento;
     * de él salen el nombre del punto y la ciudad, y contra él se validan las reglas
     * del recorrido. {@code ocurridoEn} es opcional: si el punto de la cadena no lo
     * reporta, se asume que el movimiento acaba de ocurrir. {@code claveIdempotencia}
     * también es opcional: si el cliente la envía, un reintento con la misma clave no
     * registra el movimiento dos veces.
     */
    public record Command(String trackingNumber, EventType tipo, Long centroId, String observaciones,
            String repartidorNombre, Instant ocurridoEn, String claveIdempotencia) {
    }

    /** El eventId es una columna de texto; una clave más larga no tiene uso legítimo. */
    static final int LARGO_MAXIMO_CLAVE = 100;

    private final TrackedShipmentRepository trackedShipments;
    private final LogisticsEventRepository logisticsEvents;
    private final ConsultarEstadoDelEnvio estadoDelEnvio;
    private final CatalogoDeCentros centros;
    private final CatalogoDeCiudades ciudades;
    private final EventoLogisticoPublisher publisher;
    private final Clock clock;

    public AdmitirEventoLogistico(TrackedShipmentRepository trackedShipments,
            LogisticsEventRepository logisticsEvents, ConsultarEstadoDelEnvio estadoDelEnvio,
            CatalogoDeCentros centros, CatalogoDeCiudades ciudades, EventoLogisticoPublisher publisher,
            Clock clock) {
        this.trackedShipments = trackedShipments;
        this.logisticsEvents = logisticsEvents;
        this.estadoDelEnvio = estadoDelEnvio;
        this.centros = centros;
        this.ciudades = ciudades;
        this.publisher = publisher;
        this.clock = clock;
    }

    public EventoLogisticoEntrante ejecutar(Command command) {
        TrackedShipment envio = trackedShipments.porTrackingNumber(command.trackingNumber())
                .orElseThrow(() -> new UnknownShipmentException(command.trackingNumber()));

        // Un reintento (por ejemplo, tras un timeout) con una clave que ya se registró
        // recibe el mismo movimiento en vez de crear otro. Se resuelve antes de validar
        // el recorrido: el envío ya avanzó con ese movimiento y validarlo de nuevo lo
        // rechazaría como transición inválida.
        String clave = normalizar(command.claveIdempotencia());
        if (clave != null) {
            Optional<LogisticsEvent> yaRegistrado = logisticsEvents.porEventId(clave);
            if (yaRegistrado.isPresent()) {
                return comoEntrante(yaRegistrado.get(), command.trackingNumber());
            }
        }

        Instant ahora = clock.instant();
        Instant ocurridoEn = command.ocurridoEn() == null ? ahora : command.ocurridoEn();

        if (ocurridoEn.isAfter(ahora)) {
            throw new FechaDeMovimientoInvalidaException(ocurridoEn, ahora);
        }

        List<LogisticsEvent> historial = logisticsEvents.findHistorial(command.trackingNumber());
        EstadoDelEnvio estado = estadoDelEnvio.desdeHistorial(envio, historial);

        validarRecorrido(command, estado, ocurridoEn);

        Centro centro = centros.exigir(command.centroId());
        Ciudad ciudadCentro = ciudades.exigir(centro.getCityId());
        validarCiudadDelCentro(command.tipo(), estado, centro, ciudadCentro);

        EventoLogisticoEntrante evento = new EventoLogisticoEntrante(
                clave != null ? clave : UUID.randomUUID().toString(),
                command.trackingNumber(),
                command.tipo(),
                centro.getId(),
                centro.getName(),
                ciudadCentro.etiqueta(),
                command.observaciones(),
                command.repartidorNombre(),
                ocurridoEn);

        publisher.publicar(evento);

        return evento;
    }

    private static String normalizar(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        String recortada = clave.trim();
        if (recortada.length() > LARGO_MAXIMO_CLAVE) {
            throw new ClaveDeIdempotenciaInvalidaException(LARGO_MAXIMO_CLAVE);
        }
        return recortada;
    }

    private static EventoLogisticoEntrante comoEntrante(LogisticsEvent evento, String trackingNumber) {
        if (!evento.getTrackingNumber().equals(trackingNumber)) {
            throw new ClaveDeIdempotenciaEnUsoException(evento.getEventId(), trackingNumber);
        }
        return new EventoLogisticoEntrante(
                evento.getEventId(),
                evento.getTrackingNumber(),
                evento.getType(),
                evento.getCenterId(),
                evento.getPoint(),
                evento.getCityName(),
                evento.getNotes(),
                evento.getDelivererName(),
                evento.getOccurredAt());
    }

    /** El movimiento debe caber en el recorrido: en el orden correcto y sin retroceder en el tiempo. */
    private void validarRecorrido(Command command, EstadoDelEnvio estado, Instant ocurridoEn) {
        if (!FlujoLogistico.permite(estado.ultimoTipo(), command.tipo())) {
            throw new TransicionInvalidaException(command.trackingNumber(), estado.estado(), command.tipo(),
                    estado.siguientesPermitidos());
        }

        if (estado.ultimoMovimientoEn() != null && ocurridoEn.isBefore(estado.ultimoMovimientoEn())) {
            throw new MovimientoFueraDeOrdenException(command.trackingNumber(), ocurridoEn,
                    estado.ultimoMovimientoEn());
        }
    }

    /**
     * El centro debe estar donde el recorrido lo exige: el primer ingreso en la ciudad
     * de origen, el despacho desde donde está el paquete, y la llegada, el reparto y la
     * entrega en la ciudad de destino.
     */
    private void validarCiudadDelCentro(EventType tipo, EstadoDelEnvio estado, Centro centro, Ciudad ciudadCentro) {
        CiudadEsperada esperada = FlujoLogistico.ciudadEsperada(tipo, estado.ultimoTipo());

        if (esperada == CiudadEsperada.HUB_INTERMEDIO) {
            // Un paso intermedio en la ciudad de destino no es un paso: es la llegada.
            if (centro.getCityId().equals(estado.destino().id())) {
                throw CentroFueraDeCiudadException.para(tipo, esperada, centro.getName(), ciudadCentro.etiqueta(),
                        null);
            }
            return;
        }

        Ciudad debeEstarEn = estado.ciudadQueExige(esperada);
        if (!centro.getCityId().equals(debeEstarEn.id())) {
            throw CentroFueraDeCiudadException.para(tipo, esperada, centro.getName(), ciudadCentro.etiqueta(),
                    debeEstarEn.etiqueta());
        }
    }
}
