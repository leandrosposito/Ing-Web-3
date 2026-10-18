package ar.edu.iua.iw3.controller;

import ar.edu.iua.iw3.dto.CambioEstadoResponse;
import ar.edu.iua.iw3.dto.ConciliacionResponse;
import ar.edu.iua.iw3.dto.DetalleCargaResponse;
import ar.edu.iua.iw3.dto.ErrorResponse;
import ar.edu.iua.iw3.dto.OrdenResponse;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.service.CargaService;
import ar.edu.iua.iw3.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Consultas de solo lectura (no las pide el enunciado, pero sirven para verificar cada paso y para el frontend). */
@RestController
@RequestMapping("/api/v1/ordenes")
@RequiredArgsConstructor
@Tag(name = "4. Consultas", description = "Consulta de ordenes, auditoria, detalle y conciliacion")
public class ConsultaController {

    private final OrdenService ordenService;
    private final CargaService cargaService;

    @GetMapping
    @Operation(summary = "Listar ordenes", description = "Opcionalmente filtradas por estado.")
    public List<OrdenResponse> listar(
            @Parameter(description = "Filtro opcional por estado") @RequestParam(required = false) EstadoOrden estado) {
        return ordenService.listar(estado);
    }

    @GetMapping("/{numeroOrden}")
    @Operation(summary = "Consultar una orden", description = "Incluye la cabecera con los ultimos valores de carga.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public OrdenResponse buscar(@PathVariable Long numeroOrden) {
        return ordenService.buscar(numeroOrden);
    }

    @GetMapping("/{numeroOrden}/historial-estados")
    @Operation(summary = "Log de auditoria de estados", description = "Todos los cambios de estado de la orden, en orden cronologico.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<CambioEstadoResponse> historial(@PathVariable Long numeroOrden) {
        return ordenService.historialEstados(numeroOrden);
    }

    @GetMapping("/{numeroOrden}/conciliacion")
    @Operation(summary = "Punto 5 - Consultar conciliacion", description = "Solo disponible para ordenes en estado 4: Finalizada.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La orden todavia no esta finalizada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ConciliacionResponse conciliacion(@PathVariable Long numeroOrden) {
        return ordenService.obtenerConciliacion(numeroOrden);
    }

    @GetMapping("/{numeroOrden}/detalle")
    @Operation(summary = "Detalle de carga almacenado", description = "Registros historicos guardados segun la frecuencia configurada.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<DetalleCargaResponse> detalle(@PathVariable Long numeroOrden) {
        return cargaService.listarDetalle(numeroOrden);
    }
}
