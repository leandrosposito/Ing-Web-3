package ar.edu.iua.iw3.controller;

import ar.edu.iua.iw3.dto.ErrorResponse;
import ar.edu.iua.iw3.dto.OrdenResponse;
import ar.edu.iua.iw3.dto.OrdenSapRequest;
import ar.edu.iua.iw3.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Endpoints que consume SAP (punto 1). */
@RestController
@RequestMapping("/api/v1/ordenes")
@RequiredArgsConstructor
@Tag(name = "1. SAP", description = "Recepcion de ordenes desde el sistema externo")
public class SapController {

    private final OrdenService ordenService;

    @PostMapping
    @Operation(summary = "Punto 1 - Recibir orden desde SAP",
            description = "Sincroniza camion, chofer, cliente y producto por codigo externo (los crea o actualiza) "
                    + "y crea la orden en estado 1: Pendiente de pesaje inicial.")
    @ApiResponse(responseCode = "201", description = "Orden creada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe una orden con ese numero",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody OrdenSapRequest request) {
        OrdenResponse orden = ordenService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/ordenes/" + orden.numeroOrden())).body(orden);
    }
}
