package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Camion tal como lo manda SAP. */
public record CamionDto(
        @Schema(example = "CAM-001") @NotBlank @Size(max = 50) String codigoExterno,
        @Schema(example = "AB123CD") @NotBlank @Size(max = 20) String patente,
        @Schema(example = "Scania cisterna doble") String descripcion,
        @NotEmpty @Valid List<CisternaDto> cisternas) {
}
