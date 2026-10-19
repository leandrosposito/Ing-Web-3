package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.DetalleCargaRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasDetalleCargaTest {

    private static DetalleCargaRequest dato(double masa, double densidad, double caudal) {
        return new DetalleCargaRequest(masa, densidad, 20.0, caudal);
    }

    @Test
    void aceptaDatoValido() {
        assertTrue(ReglasDetalleCarga.motivoDescarte(dato(100, 0.5, 5000), null).isEmpty());
        assertTrue(ReglasDetalleCarga.motivoDescarte(dato(200, 0.5, 5000), 100.0).isEmpty());
    }

    @Test
    void aceptaMasaIgualALaAnterior() {
        assertTrue(ReglasDetalleCarga.motivoDescarte(dato(100, 0.5, 5000), 100.0).isEmpty());
    }

    @Test
    void descartaCaudalCeroONegativo() {
        assertFalse(ReglasDetalleCarga.motivoDescarte(dato(100, 0.5, 0), null).isEmpty());
        assertFalse(ReglasDetalleCarga.motivoDescarte(dato(100, 0.5, -10), null).isEmpty());
    }

    @Test
    void descartaMasaCeroONegativa() {
        assertFalse(ReglasDetalleCarga.motivoDescarte(dato(0, 0.5, 5000), null).isEmpty());
        assertFalse(ReglasDetalleCarga.motivoDescarte(dato(-1, 0.5, 5000), null).isEmpty());
    }

    @Test
    void descartaMasaMenorALaAnterior() {
        assertFalse(ReglasDetalleCarga.motivoDescarte(dato(90, 0.5, 5000), 100.0).isEmpty());
    }

    @Test
    void descartaDensidadFueraDeRango() {
        assertFalse(ReglasDetalleCarga.motivoDescarte(dato(100, 0, 5000), null).isEmpty());
        assertFalse(ReglasDetalleCarga.motivoDescarte(dato(100, 1.2, 5000), null).isEmpty());
        assertTrue(ReglasDetalleCarga.motivoDescarte(dato(100, 1, 5000), null).isEmpty());
    }
}
