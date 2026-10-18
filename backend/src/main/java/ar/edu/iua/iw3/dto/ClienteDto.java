package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteDto(
        @Schema(example = "CLI-001") @NotBlank @Size(max = 50) String codigoExterno,
        @Schema(example = "Gas del Centro S.A.") @NotBlank String razonSocial,
        @Schema(example = "compras@gasdelcentro.com") String contacto) {
}
