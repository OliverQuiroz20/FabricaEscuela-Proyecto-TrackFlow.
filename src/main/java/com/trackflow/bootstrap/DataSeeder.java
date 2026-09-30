package com.trackflow.bootstrap;

import com.trackflow.modules.logistics.application.CatalogoDeCentros;
import com.trackflow.modules.logistics.application.EventoLogisticoEntrante;
import com.trackflow.modules.logistics.application.EventoLogisticoPublisher;
import com.trackflow.modules.logistics.domain.Centro;
import com.trackflow.modules.logistics.domain.EventType;
import com.trackflow.modules.shipments.application.EnvioSolicitado;
import com.trackflow.modules.shipments.application.EnvioSolicitadoPublisher;
import com.trackflow.modules.shipments.application.ShipmentRepository;
import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.modules.shipments.domain.TrackingNumber;
import com.trackflow.shared.geografia.CatalogoDeCiudades;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Carga los envíos semilla: los tres del plan de calidad (sección 6.5), que permiten
 * probar HU-02 y HU-03 sin depender de haber ejecutado HU-01 antes, y un conjunto de
 * envíos con datos realistas entre las cinco ciudades que tienen centros, para que
 * las consultas y los reportes muestren una operación creíble.
 *
 * Las personas, empresas, documentos, teléfonos y direcciones son inventados: tienen
 * el formato de los reales (y los NIT su dígito de verificación correcto), pero no
 * corresponden a nadie. Nunca se deben cargar datos personales reales como semilla.
 *
 * Publica en las mismas colas que cualquier otro productor: los datos de prueba
 * entran por el mismo camino que los reales, sin puerta trasera a la base de datos.
 */
@Component
@ConditionalOnProperty(name = "trackflow.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(30);

    /** Envío recién registrado, sin movimientos. */
    public static final String SIN_MOVIMIENTOS = "TF000000000001";
    /** Envío en tránsito, con historial. */
    public static final String EN_TRANSITO = "TF000000000002";
    /** Envío ya entregado. */
    public static final String ENTREGADO = "TF000000000003";

    private static final String MEDELLIN = "MEDELLÍN";
    private static final String BOGOTA = "BOGOTÁ";
    private static final String CALI = "CALI";
    private static final String BARRANQUILLA = "BARRANQUILLA";
    private static final String CARTAGENA = "CARTAGENA DE INDIAS";

    private static final String CENTRO_MEDELLIN = "Centro Norte";
    private static final String CENTRO_BOGOTA = "Centro Fontibón";
    private static final String CENTRO_CALI = "Centro Yumbo-Cali";
    private static final String CENTRO_BARRANQUILLA = "Centro Vía 40";
    private static final String CENTRO_CARTAGENA = "Centro Mamonal";

    private record Persona(String nombre, TipoDocumento tipoDocumento, String documento, String telefono,
            String direccion, String ciudad) {
    }

    /** Un movimiento del recorrido, ocurrido hace {@code horas} horas. */
    private record Movimiento(EventType tipo, String centro, int horas, String repartidor, String observaciones) {
    }

    private record Semilla(String trackingNumber, Persona remitente, Persona destinatario, String descripcion,
            int horasDesdeRegistro, List<Movimiento> movimientos) {
    }

    private final EnvioSolicitadoPublisher envios;
    private final EventoLogisticoPublisher eventos;
    private final ShipmentRepository shipments;
    private final CatalogoDeCiudades ciudades;
    private final CatalogoDeCentros centros;
    private final Clock clock;

    public DataSeeder(EnvioSolicitadoPublisher envios, EventoLogisticoPublisher eventos, ShipmentRepository shipments,
            CatalogoDeCiudades ciudades, CatalogoDeCentros centros, Clock clock) {
        this.envios = envios;
        this.eventos = eventos;
        this.shipments = shipments;
        this.ciudades = ciudades;
        this.centros = centros;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        // Cada envío se revisa por separado: así, en una base que ya tenía los tres
        // del plan de calidad, se agregan solo los que falten.
        List<Semilla> nuevas = new ArrayList<>();
        for (Semilla semilla : semillas()) {
            if (!existe(semilla.trackingNumber())) {
                solicitarEnvio(semilla);
                nuevas.add(semilla);
            }
        }

        if (nuevas.isEmpty()) {
            log.info("Datos semilla ya cargados, no se vuelven a crear");
            return;
        }

        // Los eventos se rechazan si el envío aún no está registrado, así que hay que
        // esperar a que la cola de solicitudes termine de procesarse.
        for (Semilla semilla : nuevas) {
            if (!semilla.movimientos().isEmpty()) {
                esperarA(semilla.trackingNumber());
            }
        }

        for (Semilla semilla : nuevas) {
            int orden = 1;
            for (Movimiento movimiento : semilla.movimientos()) {
                publicarEvento("seed-evt-" + semilla.trackingNumber() + "-" + orden++,
                        semilla.trackingNumber(), movimiento);
            }
        }

        log.info("Datos semilla encolados: {} envíos nuevos", nuevas.size());
    }

    /**
     * Cada recorrido sigue el orden que exige FlujoLogistico y usa centros de la
     * ciudad correcta: la semilla debe ser algo que el propio sistema habría
     * aceptado por la API, no una secuencia cualquiera. Las horas van en orden
     * descendente porque el historial se ordena por fecha de ocurrencia.
     */
    private static List<Semilla> semillas() {
        return List.of(
                // --- Los tres del plan de calidad (sección 6.5). No cambiar: los casos
                // CP-01 a CP-10 dependen de estos números, estados y personas.
                new Semilla(SIN_MOVIMIENTOS,
                        new Persona("Ana Remitente", TipoDocumento.CC, "1017254893", "3001112233",
                                "Calle 10 #20-30", MEDELLIN),
                        new Persona("Beto Destinatario", TipoDocumento.CC, "79546218", "3004445566",
                                "Carrera 7 #40-50", BOGOTA),
                        "Documentos legales", 2, List.of()),
                new Semilla(EN_TRANSITO,
                        new Persona("Ana Remitente", TipoDocumento.CC, "1017254893", "3001112233",
                                "Calle 10 #20-30", MEDELLIN),
                        new Persona("Beto Destinatario", TipoDocumento.CC, "79546218", "3004445566",
                                "Carrera 7 #40-50", BOGOTA),
                        "Repuestos industriales", 10, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_MEDELLIN, 9, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_MEDELLIN, 8, null, null))),
                new Semilla(ENTREGADO,
                        new Persona("Ana Remitente", TipoDocumento.CC, "1017254893", "3001112233",
                                "Calle 10 #20-30", MEDELLIN),
                        new Persona("Beto Destinatario", TipoDocumento.CC, "79546218", "3004445566",
                                "Carrera 7 #40-50", BOGOTA),
                        "Equipo médico", 36, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_MEDELLIN, 35, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_MEDELLIN, 33, null, null),
                                new Movimiento(EventType.ARRIVED_AT_DESTINATION_CENTER, CENTRO_BOGOTA, 9, null, null),
                                new Movimiento(EventType.OUT_FOR_DELIVERY, CENTRO_BOGOTA, 5, "Carlos Repartidor",
                                        null),
                                new Movimiento(EventType.DELIVERED, CENTRO_BOGOTA, 2, null, null))),

                // --- Operación realista: datos inventados con formato real.
                new Semilla("TF000000000004",
                        new Persona("Laura Marcela Rincón Pardo", TipoDocumento.CC, "1020745381", "3157204418",
                                "Calle 127 #15-42, Apto 503", BOGOTA),
                        new Persona("Andrés Felipe Montoya Restrepo", TipoDocumento.CC, "1036628914", "3128845120",
                                "Carrera 80 #34-15, Laureles", MEDELLIN),
                        "Portátil y cargador", 72, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_BOGOTA, 70, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_BOGOTA, 66, null, null),
                                new Movimiento(EventType.ARRIVED_AT_DESTINATION_CENTER, CENTRO_MEDELLIN, 50, null,
                                        null),
                                new Movimiento(EventType.OUT_FOR_DELIVERY, CENTRO_MEDELLIN, 30,
                                        "Jhon Fredy Ospina", null),
                                new Movimiento(EventType.DELIVERED, CENTRO_MEDELLIN, 27, null,
                                        "Recibido por el destinatario"))),
                new Semilla("TF000000000005",
                        new Persona("Confecciones La Montaña S.A.S.", TipoDocumento.NIT, "901245873-3", "6044127730",
                                "Carrera 52 #29-47, Bodega 3, Guayabal", MEDELLIN),
                        new Persona("María Fernanda Quintero Lozano", TipoDocumento.CC, "1143857206", "3176620935",
                                "Calle 5 #38-25, San Fernando", CALI),
                        "Caja con 24 prendas de dotación", 30, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_MEDELLIN, 28, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_MEDELLIN, 26, null, null),
                                new Movimiento(EventType.ARRIVED_AT_DESTINATION_CENTER, CENTRO_CALI, 10, null, null),
                                new Movimiento(EventType.OUT_FOR_DELIVERY, CENTRO_CALI, 2, "Wilmer Andrés Caicedo",
                                        null))),
                new Semilla("TF000000000006",
                        new Persona("Carlos Alberto Mendoza De la Hoz", TipoDocumento.CC, "72234517", "3004518276",
                                "Carrera 53 #82-86, Riomar", BARRANQUILLA),
                        new Persona("Distribuidora Médica Andina S.A.S.", TipoDocumento.NIT, "900876321-3",
                                "6017458210", "Avenida Calle 26 #69-76, Bodega 12", BOGOTA),
                        "Guantes y tapabocas (10 cajas)", 20, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_BARRANQUILLA, 18, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_BARRANQUILLA, 15, null, null))),
                // Pasa por Medellín como hub intermedio antes de seguir a Cali.
                new Semilla("TF000000000007",
                        new Persona("Valentina Herrera Castro", TipoDocumento.CC, "1047482659", "3014927783",
                                "Calle 30 #17-52, Pie de la Popa", CARTAGENA),
                        new Persona("Juan Sebastián Ortiz Benavides", TipoDocumento.CC, "1107052384", "3165539021",
                                "Carrera 100 #11-60, Ciudad Jardín", CALI),
                        "Artesanías en madera", 40, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_CARTAGENA, 38, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_CARTAGENA, 35, null, null),
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_MEDELLIN, 12, null,
                                        "Transbordo en hub Medellín"))),
                new Semilla("TF000000000008",
                        new Persona("Diego Armando Valencia Mina", TipoDocumento.CC, "16789452", "3183307641",
                                "Calle 70 #7N-35, Calima", CALI),
                        new Persona("Sofía Alejandra Pérez Charris", TipoDocumento.CC, "1140869327", "3046615092",
                                "Calle 98 #52-115, Villa Country", BARRANQUILLA),
                        "Documentos notariales", 3, List.of()),
                new Semilla("TF000000000009",
                        new Persona("Gabriela Sofía Márquez Rivas", TipoDocumento.CE, "5238417", "3209183342",
                                "Calle 63 #11-45, Chapinero", BOGOTA),
                        new Persona("Rafael Enrique Barrios Julio", TipoDocumento.CC, "73189264", "3106683047",
                                "Carrera 3 #6-120, Bocagrande", CARTAGENA),
                        "Regalo de cumpleaños", 100, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_BOGOTA, 96, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_BOGOTA, 90, null, null),
                                new Movimiento(EventType.ARRIVED_AT_DESTINATION_CENTER, CENTRO_CARTAGENA, 60, null,
                                        null),
                                new Movimiento(EventType.OUT_FOR_DELIVERY, CENTRO_CARTAGENA, 50,
                                        "Luis Carlos Pacheco", null),
                                new Movimiento(EventType.DELIVERED, CENTRO_CARTAGENA, 47, null,
                                        "Recibido en portería del edificio"))),
                new Semilla("TF000000000010",
                        new Persona("Tecnología Paisa S.A.S.", TipoDocumento.NIT, "901418297-4", "6044489120",
                                "Carrera 48 #10-45, Oficina 802, El Poblado", MEDELLIN),
                        new Persona("Kelly Johana Ariza Mercado", TipoDocumento.CC, "1129574803", "3002214956",
                                "Carrera 38 #74-120, El Recreo", BARRANQUILLA),
                        "Celular de reposición por garantía", 50, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_MEDELLIN, 48, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_MEDELLIN, 44, null, null),
                                new Movimiento(EventType.ARRIVED_AT_DESTINATION_CENTER, CENTRO_BARRANQUILLA, 6,
                                        null, null))),
                new Semilla("TF000000000011",
                        new Persona("Luz Dary Mosquera Palacios", TipoDocumento.CC, "31987654", "3154478129",
                                "Carrera 15 #13-40, Guayaquil", CALI),
                        new Persona("Óscar Iván Castaño Gil", TipoDocumento.CC, "80123987", "3118754406",
                                "Transversal 60 #115-58, Suba", BOGOTA),
                        "Café especial (6 libras)", 120, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_CALI, 118, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_CALI, 112, null, null),
                                new Movimiento(EventType.ARRIVED_AT_DESTINATION_CENTER, CENTRO_BOGOTA, 90, null, null),
                                new Movimiento(EventType.OUT_FOR_DELIVERY, CENTRO_BOGOTA, 80,
                                        "Edwin Mauricio Suárez", null),
                                new Movimiento(EventType.DELIVERED, CENTRO_BOGOTA, 76, null,
                                        "Recibido por el destinatario"))),
                new Semilla("TF000000000012",
                        new Persona("Jorge Luis Fontalvo Barraza", TipoDocumento.CC, "8745123", "3013398874",
                                "Calle 45 #21-30, Boston", BARRANQUILLA),
                        new Persona("Paula Andrea Giraldo Zapata", TipoDocumento.CC, "1152447890", "3136670218",
                                "Calle 10 Sur #50FF-28, Guayabal", MEDELLIN),
                        "Repuestos de motocicleta", 16, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_BARRANQUILLA, 14, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_BARRANQUILLA, 11, null, null))),
                new Semilla("TF000000000013",
                        new Persona("Lucas Martin Dubois", TipoDocumento.PP, "19AB47261", "3224415307",
                                "Calle 70A #5-57, Apto 301", BOGOTA),
                        new Persona("Camila Andrea Rengifo Muñoz", TipoDocumento.CC, "1144098321", "3185520674",
                                "Avenida 6N #28N-10, Santa Mónica", CALI),
                        "Libros universitarios", 1, List.of()),
                new Semilla("TF000000000014",
                        new Persona("Santiago Arango Vélez", TipoDocumento.CC, "1037645219", "3104498823",
                                "Calle 33 #65B-25, Belén", MEDELLIN),
                        new Persona("Natalia Carolina Suárez Beltrán", TipoDocumento.CC, "1015462390", "3172286641",
                                "Calle 147 #9-50, Cedritos", BOGOTA),
                        "Ropa deportiva", 26, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_MEDELLIN, 24, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_MEDELLIN, 22, null, null),
                                new Movimiento(EventType.ARRIVED_AT_DESTINATION_CENTER, CENTRO_BOGOTA, 8, null, null),
                                new Movimiento(EventType.OUT_FOR_DELIVERY, CENTRO_BOGOTA, 1,
                                        "Yeison Camilo Rodríguez", null))),
                new Semilla("TF000000000015",
                        new Persona("Inversiones Caribe Azul S.A.S.", TipoDocumento.NIT, "900652841-1", "6056642210",
                                "Avenida Pedro de Heredia #31-50", CARTAGENA),
                        new Persona("Ricardo José Cabarcas Olivo", TipoDocumento.CC, "9102345", "3017784430",
                                "Carrera 43 #80-15, Alto Prado", BARRANQUILLA),
                        "Facturas y contratos", 60, List.of(
                                new Movimiento(EventType.RECEIVED_AT_CENTER, CENTRO_CARTAGENA, 58, null, null),
                                new Movimiento(EventType.DISPATCHED, CENTRO_CARTAGENA, 55, null, null),
                                new Movimiento(EventType.ARRIVED_AT_DESTINATION_CENTER, CENTRO_BARRANQUILLA, 40,
                                        null, null),
                                new Movimiento(EventType.OUT_FOR_DELIVERY, CENTRO_BARRANQUILLA, 30,
                                        "Andrés Mauricio Polo", null),
                                new Movimiento(EventType.DELIVERED, CENTRO_BARRANQUILLA, 28, null,
                                        "Recibido por el destinatario"))));
    }

    /**
     * Los movimientos se escalonan en el pasado en vez de compartir el instante de
     * arranque: el historial se ordena por fecha de ocurrencia, y con marcas
     * idénticas no habría forma de saber cuál fue el último.
     */
    private Instant hace(int horas) {
        return clock.instant().minus(horas, ChronoUnit.HOURS);
    }

    private void solicitarEnvio(Semilla semilla) {
        // Se buscan por nombre y no por identificador fijo: los ids del catálogo los
        // asigna la migración y no son parte de su contrato.
        Ciudad origen = buscarCiudad(semilla.remitente().ciudad());
        Ciudad destino = buscarCiudad(semilla.destinatario().ciudad());

        envios.publicar(new EnvioSolicitado(
                "seed-env-" + semilla.trackingNumber(),
                semilla.trackingNumber(),
                aParte(semilla.remitente(), origen),
                aParte(semilla.destinatario(), destino),
                origen,
                destino,
                semilla.descripcion(),
                hace(semilla.horasDesdeRegistro())));
    }

    private static Party aParte(Persona persona, Ciudad ciudad) {
        return new Party(persona.nombre(), persona.tipoDocumento(), persona.documento(), persona.telefono(),
                persona.direccion(), ciudad.id());
    }

    /**
     * Se exige coincidencia exacta del nombre: la búsqueda es por texto, y sin este
     * filtro "CALI" podría resolver a otra ciudad que lo contenga.
     */
    private Ciudad buscarCiudad(String nombre) {
        return ciudades.buscar(nombre, 10).stream()
                .filter(ciudad -> ciudad.nombre().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "El catálogo de ciudades no tiene '%s'; revise la migración V4".formatted(nombre)));
    }

    private Centro buscarCentro(String nombre) {
        return centros.buscar(nombre, null, 10).stream()
                .filter(centro -> centro.getName().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "El catálogo de centros no tiene '%s'; revise la migración V6".formatted(nombre)));
    }

    /** Resuelve el nombre y la ciudad del centro como lo haría AdmitirEventoLogistico. */
    private void publicarEvento(String eventId, String trackingNumber, Movimiento movimiento) {
        Centro centro = buscarCentro(movimiento.centro());
        Ciudad ciudadCentro = ciudades.exigir(centro.getCityId());

        eventos.publicar(new EventoLogisticoEntrante(
                eventId, trackingNumber, movimiento.tipo(), centro.getId(), centro.getName(),
                ciudadCentro.etiqueta(), movimiento.observaciones(), movimiento.repartidor(),
                hace(movimiento.horas())));
    }

    private boolean existe(String trackingNumber) {
        return shipments.existsByTrackingNumber(TrackingNumber.of(trackingNumber));
    }

    private void esperarA(String trackingNumber) throws InterruptedException {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA.toMillis();
        while (!existe(trackingNumber)) {
            if (System.currentTimeMillis() > limite) {
                log.warn("El envío semilla {} no se registró a tiempo; sus eventos podrían rechazarse",
                        trackingNumber);
                return;
            }
            Thread.sleep(200);
        }
    }
}
