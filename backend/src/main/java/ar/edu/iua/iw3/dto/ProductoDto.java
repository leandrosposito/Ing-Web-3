package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductoDto(
        @Schema(example = "PRO-001") @NotBlank @Size(max = 50) String codigoExterno,
        @Schema(example = "Propano") @NotBlank String nombre,
        @Schema(example = "Gas licuado de petroleo - propano") String descripcion) {
}
