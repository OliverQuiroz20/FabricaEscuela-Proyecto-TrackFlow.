package com.trackflow.modules.reports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.trackflow.modules.reports.domain.PeriodoInvalidoException;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Criterios de aceptación de "Consultar el reporte de volumen de envíos", ejercidos a
 * través de la proyección real (eventos de entrada) y el caso de uso de consulta.
 */
class ConsultarVolumenDeEnviosTest {

    /** 30 de septiembre de 2026, 15:00 en Bogotá (20:00 UTC). */
    private static final Instant AHORA = Instant.parse("2026-09-30T20:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 9, 30);
    private static final LocalDate PRIMERO_SEP = LocalDate.of(2026, 9, 1);
    private static final LocalDate QUINCE_SEP = LocalDate.of(2026, 9, 15);

    private static final long MEDELLIN = 1L;
    private static final long BOGOTA = 10L;
    private static final long CENTRO_NORTE = 1L;
    private static final long CENTRO_FONTIBON = 3L;
    private static final String ETIQUETA_MEDELLIN = "MEDELLÍN - ANTIOQUIA";
    private static final String ETIQUETA_BOGOTA = "BOGOTÁ - CUNDINAMARCA";

    private final AtomicLong secuencia = new AtomicLong();

    private RepositorioDeVolumenEnMemoria repositorio;
    private ProyectarVolumenDeEnvios proyeccion;
    private ConsultarVolumenDeEnvios consulta;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioDeVolumenEnMemoria();
        proyeccion = new ProyectarVolumenDeEnvios(repositorio);
        consulta = new ConsultarVolumenDeEnvios(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Nested
    class VolumenDeUnPeriodo {

        @Test
        void muestraElTotalYSuDesglosePorPuntoDeLaRed() {
            registrarEnvio("TF1", MEDELLIN, "2026-09-10T14:00:00Z");
            recibirEnCentro("TF1", CENTRO_NORTE, "Centro Norte", ETIQUETA_MEDELLIN, "2026-09-10T16:00:00Z");
            registrarEnvio("TF2", MEDELLIN, "2026-09-11T14:00:00Z");
            recibirEnCentro("TF2", CENTRO_NORTE, "Centro Norte", ETIQUETA_MEDELLIN, "2026-09-11T18:00:00Z");
            registrarEnvio("TF3", BOGOTA, "2026-09-12T14:00:00Z");
            recibirEnCentro("TF3", CENTRO_FONTIBON, "Centro Fontibón", ETIQUETA_BOGOTA, "2026-09-12T15:00:00Z");
            registrarEnvio("TF4", BOGOTA, "2026-09-13T14:00:00Z");

            VolumenDeEnvios volumen = consulta.ejecutar(PRIMERO_SEP, QUINCE_SEP);

            assertThat(volumen.totalRegistrados()).isEqualTo(4);
            assertThat(volumen.hayEnvios()).isTrue();
            assertThat(volumen.enCurso()).isFalse();
            assertThat(volumen.porPunto())
                    .extracting(VolumenPorPunto::punto, VolumenPorPunto::envios, VolumenPorPunto::pendienteDeRecepcion)
                    .containsExactly(
                            tuple("Centro Norte", 2L, false),
                            tuple("Centro Fontibón", 1L, false),
                            tuple("Pendiente de recepción en " + ETIQUETA_BOGOTA, 1L, true));
            assertThat(volumen.porPunto().stream().mapToLong(VolumenPorPunto::envios).sum())
                    .isEqualTo(volumen.totalRegistrados());
        }

        @Test
        void excluyeLosEnviosFueraDelPeriodo() {
            registrarEnvio("ANTES", MEDELLIN, "2026-08-31T23:00:00Z");
            registrarEnvio("DENTRO", MEDELLIN, "2026-09-05T12:00:00Z");
            registrarEnvio("DESPUES", MEDELLIN, "2026-09-16T12:00:00Z");

            VolumenDeEnvios volumen = consulta.ejecutar(PRIMERO_SEP, QUINCE_SEP);

            assertThat(volumen.totalRegistrados()).isEqualTo(1);
        }

        @Test
        void cuentaLosDiasEnHoraDeColombiaYNoEnUtc() {
            // 1 de sept. a las 03:00 UTC son las 22:00 del 31 de agosto en Bogotá.
            registrarEnvio("31-AGO-NOCHE", MEDELLIN, "2026-09-01T03:00:00Z");
            // 16 de sept. a las 04:59 UTC son las 23:59 del 15 de septiembre en Bogotá.
            registrarEnvio("15-SEP-NOCHE", MEDELLIN, "2026-09-16T04:59:00Z");

            VolumenDeEnvios volumen = consulta.ejecutar(PRIMERO_SEP, QUINCE_SEP);

            assertThat(volumen.totalRegistrados()).isEqualTo(1);
            assertThat(volumen.inicio()).isEqualTo(Instant.parse("2026-09-01T05:00:00Z"));
            assertThat(volumen.corte()).isEqualTo(Instant.parse("2026-09-16T05:00:00Z"));
        }

        @Test
        void informaLosEntregadosDelPeriodoComoCifraAparte() {
            registrarEnvio("REG-AGOSTO", MEDELLIN, "2026-08-28T14:00:00Z");
            entregar("REG-AGOSTO", "2026-09-02T14:00:00Z");
            registrarEnvio("REG-SEPT", MEDELLIN, "2026-09-03T14:00:00Z");

            VolumenDeEnvios volumen = consulta.ejecutar(PRIMERO_SEP, QUINCE_SEP);

            assertThat(volumen.totalRegistrados()).isEqualTo(1);
            assertThat(volumen.totalEntregados()).isEqualTo(1);
        }
    }

    @Nested
    class VolumenDelPeriodoEnCurso {

        @Test
        void sinFechasConsultaElDiaEnCursoHastaElMomentoDeLaConsulta() {
            registrarEnvio("HOY-MANANA", MEDELLIN, "2026-09-30T13:00:00Z");
            registrarEnvio("HOY-HACE-UN-MINUTO", BOGOTA, "2026-09-30T19:59:00Z");
            registrarEnvio("AYER", MEDELLIN, "2026-09-29T20:00:00Z");

            VolumenDeEnvios volumen = consulta.ejecutar(null, null);

            assertThat(volumen.desde()).isEqualTo(HOY);
            assertThat(volumen.hasta()).isEqualTo(HOY);
            assertThat(volumen.enCurso()).isTrue();
            assertThat(volumen.corte()).isEqualTo(AHORA);
            assertThat(volumen.totalRegistrados()).isEqualTo(2);
        }

        @Test
        void unPeriodoQueIncluyeHoyCortaEnElMomentoDeLaConsulta() {
            registrarEnvio("HOY", MEDELLIN, "2026-09-30T13:00:00Z");

            VolumenDeEnvios volumen = consulta.ejecutar(PRIMERO_SEP, HOY.plusDays(3));

            assertThat(volumen.enCurso()).isTrue();
            assertThat(volumen.corte()).isEqualTo(AHORA);
            assertThat(volumen.totalRegistrados()).isEqualTo(1);
        }
    }

    @Nested
    class PeriodoSinEnvios {

        @Test
        void informaQueNoHayEnvios() {
            registrarEnvio("OTRO-MES", MEDELLIN, "2026-07-10T14:00:00Z");

            VolumenDeEnvios volumen = consulta.ejecutar(PRIMERO_SEP, QUINCE_SEP);

            assertThat(volumen.hayEnvios()).isFalse();
            assertThat(volumen.totalRegistrados()).isZero();
            assertThat(volumen.porPunto()).isEmpty();
        }
    }

    @Nested
    class PeriodoInvalido {

        @Test
        void rechazaUnPeriodoInvertido() {
            assertThatThrownBy(() -> consulta.ejecutar(QUINCE_SEP, PRIMERO_SEP))
                    .isInstanceOf(PeriodoInvalidoException.class)
                    .hasMessageContaining("invertido");
        }

        @Test
        void rechazaUnPeriodoQueEmpiezaEnElFuturo() {
            LocalDate manana = HOY.plusDays(1);
            assertThatThrownBy(() -> consulta.ejecutar(manana, null))
                    .isInstanceOf(PeriodoInvalidoException.class)
                    .hasMessageContaining("futuro");
        }
    }

    @Nested
    class Proyeccion {

        @Test
        void elPuntoDeIngresoEsElPrimerRecibidoAunqueLleguenDesordenados() {
            registrarEnvio("TF1", MEDELLIN, "2026-09-10T14:00:00Z");
            // Un hub intermedio reportado primero, pero ocurrido después.
            recibirEnCentro("TF1", 99L, "Hub Intermedio", "OTRA", "2026-09-11T10:00:00Z");
            recibirEnCentro("TF1", CENTRO_NORTE, "Centro Norte", ETIQUETA_MEDELLIN, "2026-09-10T16:00:00Z");
            // Un "recibido" posterior no cambia el punto de ingreso.
            recibirEnCentro("TF1", 98L, "Otro Hub", "OTRA", "2026-09-12T10:00:00Z");

            VolumenDeEnvios volumen = consulta.ejecutar(PRIMERO_SEP, QUINCE_SEP);

            assertThat(volumen.porPunto()).singleElement().satisfies(p -> {
                assertThat(p.centroId()).isEqualTo(CENTRO_NORTE);
                assertThat(p.punto()).isEqualTo("Centro Norte");
            });
        }

        @Test
        void republicarElAltaNoBorraElIngresoYaAplicado() {
            registrarEnvio("TF1", MEDELLIN, "2026-09-10T14:00:00Z");
            recibirEnCentro("TF1", CENTRO_NORTE, "Centro Norte", ETIQUETA_MEDELLIN, "2026-09-10T16:00:00Z");
            registrarEnvio("TF1", MEDELLIN, "2026-09-10T14:00:00Z");

            assertThat(repositorio.findByTrackingNumber("TF1")).get()
                    .satisfies(v -> assertThat(v.getEntryCenterId()).isEqualTo(CENTRO_NORTE));
        }

        @Test
        void ignoraLosMovimientosQueNoSonIngresoNiEntrega() {
            registrarEnvio("TF1", MEDELLIN, "2026-09-10T14:00:00Z");
            proyeccion.alRegistrarEvento(evento("TF1", "DISPATCHED", CENTRO_NORTE, "Centro Norte",
                    ETIQUETA_MEDELLIN, "2026-09-10T18:00:00Z"));

            assertThat(repositorio.findByTrackingNumber("TF1")).get()
                    .satisfies(v -> assertThat(v.ingresoALaRed()).isFalse());
        }
    }

    private void registrarEnvio(String trackingNumber, long ciudadOrigenId, String registradoEn) {
        String ciudad = ciudadOrigenId == MEDELLIN ? ETIQUETA_MEDELLIN : ETIQUETA_BOGOTA;
        Instant at = Instant.parse(registradoEn);
        proyeccion.alCrearEnvio(new EnvioCreadoEvent(trackingNumber, "REGISTERED", "Remitente", ciudadOrigenId,
                ciudad, "Destinatario", "Calle 1", BOGOTA, ETIQUETA_BOGOTA, at, at));
    }

    private void recibirEnCentro(String trackingNumber, long centroId, String punto, String ciudad, String en) {
        proyeccion.alRegistrarEvento(evento(trackingNumber, "RECEIVED_AT_CENTER", centroId, punto, ciudad, en));
    }

    private void entregar(String trackingNumber, String en) {
        proyeccion.alRegistrarEvento(evento(trackingNumber, "DELIVERED", CENTRO_FONTIBON, "Centro Fontibón",
                ETIQUETA_BOGOTA, en));
    }

    private EventoLogisticoRegistradoEvent evento(String trackingNumber, String tipo, Long centroId, String punto,
            String ciudad, String en) {
        Instant at = Instant.parse(en);
        return new EventoLogisticoRegistradoEvent(secuencia.incrementAndGet(), trackingNumber, tipo, tipo, punto,
                centroId, ciudad, at, at, at);
    }
}
