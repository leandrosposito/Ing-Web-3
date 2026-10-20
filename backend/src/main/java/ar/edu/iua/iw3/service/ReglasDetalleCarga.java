package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.DetalleCargaRequest;

import java.util.Optional;

/** Cuando se descarta un dato de carga (sin dependencias, para testearla sola). */
public final class ReglasDetalleCarga {

    private ReglasDetalleCarga() {
    }

    /** Devuelve el motivo de descarte, o vacio si el dato es valido. Una masa igual a la anterior se acepta. */
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
