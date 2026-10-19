package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Punto 3.3: dato de carga en tiempo real.
 * Solo se exige que vengan los cuatro valores. Que sean validos (caudal > 0, masa creciente, etc.)
 * es una regla de negocio: si no la cumplen, el dato se descarta pero no es un error del request.
 */
public record DetalleCargaRequest(
        @Schema(description = "kg cargados hasta el momento", example = "1500") @NotNull Double masaAcumulada,
        @Schema(description = "Entre 0 y 1", example = "0.52") @NotNull Double densidad,
        @Schema(description = "Grados centigrados", example = "18.5") @NotNull Double temperatura,
        @Schema(description = "kg/h", example = "60000") @NotNull Double caudal) {
}
