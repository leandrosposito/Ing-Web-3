package ar.edu.iua.iw3.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Punto 4: el body es opcional; solo sirve para dejar una observacion en la auditoria. */
public record CierreRequest(
        @Schema(example = "Carga completa") @Size(max = 500) String observaciones) {
}
