package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.DetalleCargaRequest;

import java.util.Optional;

/**
 * Reglas de validez de un dato de carga (punto 3). Es una clase sin dependencias para poder testearla sola.
 * Se descarta el dato si:
 * - caudal <= 0
 * - masa acumulada <= 0 o menor que la anterior (igual se acepta: la bomba puede no haber avanzado)
 * - densidad fuera de (0, 1]  (el enunciado define ese rango; un valor fuera de el no es valido)
 */
public final class ReglasDetalleCarga {

    private ReglasDetalleCarga() {
    }

    /** @return el motivo de descarte, o vacio si el dato es valido. */
    public static Optional<String> motivoDescarte(DetalleCargaRequest dato, Double masaAnterior) {
        if (dato.caudal() <= 0) {
            return Optional.of("Caudal debe ser mayor a 0");
        }
        if (dato.masaAcumulada() <= 0) {
            return Optional.of("Masa acumulada debe ser mayor a 0");
        }
        if (masaAnterior != null && dato.masaAcumulada() < masaAnterior) {
            return Optional.of("Masa acumulada (" + dato.masaAcumulada() + ") menor que la anterior (" + masaAnterior + ")");
        }
        if (dato.densidad() <= 0 || dato.densidad() > 1) {
            return Optional.of("Densidad debe estar entre 0 y 1");
        }
        return Optional.empty();
    }
}
