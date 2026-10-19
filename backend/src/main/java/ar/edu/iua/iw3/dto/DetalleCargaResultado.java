package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta a cada dato de carga recibido. */
public record DetalleCargaResultado(
        Long numeroOrden,
        @Schema(description = "true si el dato es valido y actualizo la cabecera de la orden") boolean aceptado,
        @Schema(description = "true si ademas se guardo como registro de detalle (depende de la frecuencia configurada)") boolean almacenado,
        @Schema(description = "Motivo por el que se descarto el dato (null si fue aceptado)") String motivoDescarte) {

    public static DetalleCargaResultado descartado(Long numeroOrden, String motivo) {
        return new DetalleCargaResultado(numeroOrden, false, false, motivo);
    }

    public static DetalleCargaResultado aceptado(Long numeroOrden, boolean almacenado) {
        return new DetalleCargaResultado(numeroOrden, true, almacenado, null);
    }
}
