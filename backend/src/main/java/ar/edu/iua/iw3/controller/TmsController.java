package ar.edu.iua.iw3.controller;

import ar.edu.iua.iw3.dto.ConciliacionResponse;
import ar.edu.iua.iw3.dto.ErrorResponse;
import ar.edu.iua.iw3.dto.PesajeInicialResponse;
import ar.edu.iua.iw3.dto.PesajeRequest;
import ar.edu.iua.iw3.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Endpoints que consume la balanza (TMS): puntos 2 y 5. */
@RestController
@RequestMapping("/api/v1/ordenes")
@RequiredArgsConstructor
@Tag(name = "2. TMS (balanza)", description = "Pesaje inicial (tara) y pesaje final")
public class TmsController {

    private final OrdenService ordenService;

    @PostMapping("/{numeroOrden}/pesaje-inicial")
    @Operation(summary = "Punto 2 - Registrar pesaje inicial (tara)",
            description = "Registra el peso del camion vacio, genera la contrasena de activacion de 5 digitos "
                    + "y pasa la orden a estado 2. Solo para ordenes en estado 1.")
    @ApiResponse(responseCode = "200", description = "Pesaje registrado; incluye la contrasena de activacion")
    @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La orden no esta en estado 1",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PesajeInicialResponse pesajeInicial(@PathVariable Long numeroOrden, @Valid @RequestBody PesajeRequest request) {
        return ordenService.registrarPesajeInicial(numeroOrden, request);
    }

    @PostMapping("/{numeroOrden}/pesaje-final")
    @Operation(summary = "Punto 5 - Registrar pesaje final y obtener la conciliacion",
            description = "Registra el peso del camion cargado, pasa la orden a estado 4: Finalizada y devuelve la "
                    + "conciliacion. Solo para ordenes en estado 3.")
    @ApiResponse(responseCode = "200", description = "Pesaje registrado; se devuelve la conciliacion")
    @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La orden no esta en estado 3",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ConciliacionResponse pesajeFinal(@PathVariable Long numeroOrden, @Valid @RequestBody PesajeRequest request) {
        return ordenService.registrarPesajeFinal(numeroOrden, request);
    }
}
