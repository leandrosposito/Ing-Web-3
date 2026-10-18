package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Se usa tanto para recibir el chofer desde SAP como para devolverlo (la forma es la misma). */
public record ChoferDto(
        @Schema(example = "CHO-001") @NotBlank @Size(max = 50) String codigoExterno,
        @Schema(example = "Juan") @NotBlank String nombre,
        @Schema(example = "Perez") @NotBlank String apellido,
        @Schema(example = "30123456") String documento) {
}
