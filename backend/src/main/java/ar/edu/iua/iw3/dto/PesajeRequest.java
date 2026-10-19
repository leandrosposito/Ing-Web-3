package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Puntos 2 y 5: pesaje enviado por la balanza (TMS). */
public record PesajeRequest(
        @Schema(description = "Peso del camion en kg", example = "15000") @NotNull @Positive Double pesaje,
        @Schema(description = "Opcional, queda en el log de auditoria") @Size(max = 500) String observaciones) {
}
