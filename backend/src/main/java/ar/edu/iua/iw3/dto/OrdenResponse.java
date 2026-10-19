package ar.edu.iua.iw3.dto;

import ar.edu.iua.iw3.model.EstadoOrden;

import java.time.LocalDateTime;

/** Vista completa de la orden. No incluye la contrasena de activacion. */
public record OrdenResponse(
        Long numeroOrden,
        EstadoOrden estado,
        int estadoNumero,
        String estadoDescripcion,
        CamionResponse camion,
        ChoferDto chofer,
        ClienteDto cliente,
        ProductoDto producto,
        LocalDateTime fechaPrevistaCarga,
        Double preset,
        LocalDateTime fechaRecepcion,
        Double pesajeInicial,
        LocalDateTime fechaPesajeInicial,
        LocalDateTime fechaInicioCarga,
        LocalDateTime fechaFinCarga,
        Double ultimaMasaAcumulada,
        Double ultimaDensidad,
        Double ultimaTemperatura,
        Double ultimoCaudal,
        LocalDateTime fechaUltimoDato,
        Double pesajeFinal,
        LocalDateTime fechaPesajeFinal) {
}
