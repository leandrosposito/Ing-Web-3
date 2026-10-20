package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Solo se exige que vengan los cuatro valores; si no son validos el dato se descarta, no es un error 400. */
public record DetalleCargaRequest(
        @Schema(description = "kg cargados hasta el momento", example = "1500") @NotNull Double masaAcumulada,
        @Schema(description = "Entre 0 y 1", example = "0.52") @NotNull Double densidad,
        @Schema(description = "Grados centigrados", example = "18.5") @NotNull Double temperatura,
        @Schema(description = "kg/h", example = "60000") @NotNull Double caudal) {
}
