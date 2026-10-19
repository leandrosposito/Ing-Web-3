package ar.edu.iua.iw3.model;

import org.junit.jupiter.api.Test;

import static ar.edu.iua.iw3.model.EstadoOrden.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoOrdenTest {

    @Test
    void permiteSoloElEstadoSiguiente() {
        assertTrue(PENDIENTE_PESAJE_INICIAL.puedePasarA(CON_PESAJE_INICIAL));
        assertTrue(CON_PESAJE_INICIAL.puedePasarA(CERRADA_PARA_CARGA));
        assertTrue(CERRADA_PARA_CARGA.puedePasarA(FINALIZADA));
    }

    @Test
    void noPermiteSaltearEstados() {
        assertFalse(PENDIENTE_PESAJE_INICIAL.puedePasarA(CERRADA_PARA_CARGA));
        assertFalse(CON_PESAJE_INICIAL.puedePasarA(FINALIZADA));
    }

    @Test
    void noPermiteRetrocederNiQuedarseEnElMismo() {
        assertFalse(CON_PESAJE_INICIAL.puedePasarA(PENDIENTE_PESAJE_INICIAL));
        assertFalse(FINALIZADA.puedePasarA(CERRADA_PARA_CARGA));
        assertFalse(CON_PESAJE_INICIAL.puedePasarA(CON_PESAJE_INICIAL));
        assertFalse(FINALIZADA.puedePasarA(null));
    }
}
