package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CisternaDto(
        @Schema(example = "1") @NotNull @Positive Integer numero,
        @Schema(description = "Capacidad en litros", example = "10000") @NotNull @Positive Double capacidadLitros) {
}
