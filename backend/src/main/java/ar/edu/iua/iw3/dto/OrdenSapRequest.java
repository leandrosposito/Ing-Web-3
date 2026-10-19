package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/** Punto 1: datos base de la orden enviados por SAP. */
public record OrdenSapRequest(
        @Schema(example = "1001") @NotNull @Positive Long numeroOrden,
        @NotNull @Valid CamionDto camion,
        @NotNull @Valid ChoferDto chofer,
        @NotNull @Valid ClienteDto cliente,
        @NotNull @Valid ProductoDto producto,
        @Schema(description = "Turno de carga", example = "2026-11-10T08:00:00") @NotNull LocalDateTime fechaPrevistaCarga,
        @Schema(description = "Kilogramos a cargar", example = "20000") @NotNull @Positive Double preset) {
}
