package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Punto 3.1: el sistema de carga manda la contrasena de activacion. */
public record HabilitacionCargaRequest(
        @Schema(example = "48213") @NotNull Integer password) {
}
