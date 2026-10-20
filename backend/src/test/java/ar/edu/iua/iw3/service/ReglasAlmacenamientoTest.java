package ar.edu.iua.iw3.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasAlmacenamientoTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 1, 10, 0, 0);

    @Test
    void conFrecuenciaDiezSegundosYUnDatoPorSegundoSeGuardanSeisEnUnMinuto() {
        LocalDateTime ultimoGuardado = null;
        int guardados = 0;
        for (int segundo = 0; segundo < 60; segundo++) {
            LocalDateTime ahora = INICIO.plusSeconds(segundo);
            if (ReglasAlmacenamiento.debeAlmacenar(ultimoGuardado, ahora, 10)) {
                guardados++;
                ultimoGuardado = ahora;
            }
        }
        assertEquals(6, guardados); // segundos 0, 10, 20, 30, 40 y 50
    }

    @Test
    void elPrimerDatoSiempreSeGuarda() {
        assertTrue(ReglasAlmacenamiento.debeAlmacenar(null, INICIO, 10));
    }

    @Test
    void noSeGuardaAntesDeQuePaseLaFrecuencia() {
        assertFalse(ReglasAlmacenamiento.debeAlmacenar(INICIO, INICIO.plusSeconds(9), 10));
        assertTrue(ReglasAlmacenamiento.debeAlmacenar(INICIO, INICIO.plusSeconds(10), 10));
    }

    @Test
    void conFrecuenciaCeroSeGuardaTodo() {
        assertTrue(ReglasAlmacenamiento.debeAlmacenar(INICIO, INICIO, 0));
        assertTrue(ReglasAlmacenamiento.debeAlmacenar(INICIO, INICIO.plusSeconds(1), 0));
    }
}
