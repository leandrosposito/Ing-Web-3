package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Punto 5: se calcula a partir de la orden y su detalle; no se guarda en la base. */
public record ConciliacionResponse(
        Long numeroOrden,
        @Schema(description = "Tara (kg)") Double pesajeInicial,
        @Schema(description = "kg") Double pesajeFinal,
        @Schema(description = "Ultima masa acumulada medida por el caudalimetro (kg)") Double productoCargado,
        @Schema(description = "Pesaje final - pesaje inicial (kg)") Double netoPorBalanza,
        @Schema(description = "Neto por balanza - producto cargado (kg)") Double diferenciaBalanzaCaudalimetro,
        @Schema(description = "Promedio de los datos almacenados (null si no hay detalle)") Double promedioTemperatura,
        Double promedioDensidad,
        Double promedioCaudal) {
}
