package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.model.EstadoOrden;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CalculosMonitoreoTest {

    private static final EstadoOrden CARGANDO = EstadoOrden.CON_PESAJE_INICIAL;

    @Test
    void etaEsLoQueFaltaDividoElCaudal() {
        // faltan 6000 kg a 3000 kg/h -> 2 horas
        assertEquals(2.0, CalculosMonitoreo.etaHoras(CARGANDO, 10000.0, 4000.0, 3000.0));
    }

    @Test
    void etaEsCeroSiYaSeLlegoAlPreset() {
        assertEquals(0.0, CalculosMonitoreo.etaHoras(CARGANDO, 10000.0, 10500.0, 3000.0));
    }

    @Test
    void etaNoSeCalculaSinCaudalValidoOSinDatos() {
        assertNull(CalculosMonitoreo.etaHoras(CARGANDO, 10000.0, 4000.0, 0.0));
        assertNull(CalculosMonitoreo.etaHoras(CARGANDO, 10000.0, 4000.0, null));
        assertNull(CalculosMonitoreo.etaHoras(CARGANDO, 10000.0, null, 3000.0));
    }

    @Test
    void etaSoloParaOrdenesEnEstado2() {
        assertNull(CalculosMonitoreo.etaHoras(EstadoOrden.PENDIENTE_PESAJE_INICIAL, 10000.0, 4000.0, 3000.0));
        assertNull(CalculosMonitoreo.etaHoras(EstadoOrden.CERRADA_PARA_CARGA, 10000.0, 4000.0, 3000.0));
        assertNull(CalculosMonitoreo.etaHoras(EstadoOrden.FINALIZADA, 10000.0, 4000.0, 3000.0));
    }

    @Test
    void tiempoTranscurridoEsLaDiferenciaConElInicioDeCarga() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 8, 0, 0);
        LocalDateTime ahora = inicio.plusMinutes(5).plusSeconds(30);
        assertEquals(330L, CalculosMonitoreo.tiempoTranscurridoSegundos(CARGANDO, inicio, ahora));
    }

    @Test
    void tiempoTranscurridoSoloEnEstado2YConCargaIniciada() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 8, 0, 0);
        LocalDateTime ahora = inicio.plusMinutes(1);
        assertNull(CalculosMonitoreo.tiempoTranscurridoSegundos(CARGANDO, null, ahora));
        assertNull(CalculosMonitoreo.tiempoTranscurridoSegundos(EstadoOrden.CERRADA_PARA_CARGA, inicio, ahora));
        assertNull(CalculosMonitoreo.tiempoTranscurridoSegundos(EstadoOrden.FINALIZADA, inicio, ahora));
    }
}
