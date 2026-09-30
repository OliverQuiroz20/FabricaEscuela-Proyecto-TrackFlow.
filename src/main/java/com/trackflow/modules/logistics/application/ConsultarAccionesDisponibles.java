package com.trackflow.modules.logistics.application;

import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.CiudadEsperada;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.logistics.domain.FlujoLogistico;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Qué puede hacer el operador con este envío, ahora mismo.
 *
 * Responde con los movimientos admisibles y, para cada uno, los centros concretos
 * entre los que puede escoger y la ruta que le corresponde. La interfaz no tiene que
 * decidir nada ni conocer el recorrido: pinta lo que le llega. Como las opciones
 * salen de las mismas reglas que después validan el registro
 * ({@link FlujoLogistico}), es imposible que se ofrezca algo que vaya a rechazarse.
 */
@Service
public class ConsultarAccionesDisponibles {

    private static final int LIMITE_CENTROS = 20;

    /** Un centro con su ciudad ya resuelta, listo para mostrarse. */
    public record CentroDisponible(Centro centro, Ciudad ciudad) {
    }

    /**
     * @param ciudadDeLosCentros ciudad a la que pertenecen los centros ofrecidos, o
     *        null en el paso por un hub, donde vale cualquiera menos la de destino
     * @param ruta trayecto que representa el movimiento, solo para el despacho
     */
    public record Accion(EventType tipo, CiudadEsperada ciudadEsperada, Ciudad ciudadDeLosCentros,
            List<CentroDisponible> centros, String ruta) {
    }

    public record Acciones(EstadoDelEnvio estado, List<Accion> acciones) {
    }

    private final ConsultarEstadoDelEnvio estadoDelEnvio;
    private final CatalogoDeCentros centros;
    private final CatalogoDeCiudades ciudades;

    public ConsultarAccionesDisponibles(ConsultarEstadoDelEnvio estadoDelEnvio, CatalogoDeCentros centros,
            CatalogoDeCiudades ciudades) {
        this.estadoDelEnvio = estadoDelEnvio;
        this.centros = centros;
        this.ciudades = ciudades;
    }

    public Acciones ejecutar(String trackingNumber) {
        EstadoDelEnvio estado = estadoDelEnvio.ejecutar(trackingNumber);

        List<Accion> acciones = estado.siguientesPermitidos().stream()
                .map(tipo -> construir(tipo, estado))
                .toList();

        return new Acciones(estado, acciones);
    }

    private Accion construir(EventType tipo, EstadoDelEnvio estado) {
        CiudadEsperada esperada = FlujoLogistico.ciudadEsperada(tipo, estado.ultimoTipo());
        Ciudad ciudad = estado.ciudadQueExige(esperada);

        return new Accion(tipo, esperada, ciudad, centrosPara(ciudad, estado), rutaPara(tipo, estado));
    }

    private List<CentroDisponible> centrosPara(Ciudad ciudad, EstadoDelEnvio estado) {
        // Con ciudad concreta basta con filtrar por ella; en el paso por un hub la
        // regla es al revés — sirve cualquiera menos la de destino, porque un centro
        // de la ciudad de destino significa que el paquete ya llegó.
        List<Centro> disponibles = ciudad != null
                ? centros.buscar(null, ciudad.id(), LIMITE_CENTROS)
                : centros.buscar(null, null, LIMITE_CENTROS).stream()
                        .filter(centro -> !centro.getCityId().equals(estado.destino().id()))
                        .toList();

        return disponibles.stream()
                .map(centro -> new CentroDisponible(centro,
                        ciudad != null ? ciudad : ciudades.exigir(centro.getCityId())))
                .toList();
    }

    /**
     * El despacho es el único movimiento que representa un trayecto, y sale de las
     * entidades: desde donde está el paquete hacia el destino del envío. El operador
     * no la escribe.
     */
    private String rutaPara(EventType tipo, EstadoDelEnvio estado) {
        if (tipo != EventType.DISPATCHED) {
            return null;
        }
        return estado.ciudadActual().etiqueta() + " → " + estado.destino().etiqueta();
    }
}
