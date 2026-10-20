package ar.edu.iua.iw3.controller;

import ar.edu.iua.iw3.dto.CierreRequest;
import ar.edu.iua.iw3.dto.DetalleCargaRequest;
import ar.edu.iua.iw3.dto.DetalleCargaResultado;
import ar.edu.iua.iw3.dto.ErrorResponse;
import ar.edu.iua.iw3.dto.HabilitacionCargaRequest;
import ar.edu.iua.iw3.dto.HabilitacionCargaResponse;
import ar.edu.iua.iw3.dto.OrdenResponse;
import ar.edu.iua.iw3.service.CargaService;
import ar.edu.iua.iw3.service.OrdenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Endpoints que consume el sistema de control de carga: puntos 3 y 4. */
@RestController
@RequestMapping("/api/v1/ordenes")
@RequiredArgsConstructor
@Tag(name = "3. Sistema de carga", description = "Habilitacion, datos en tiempo real y cierre de la carga")
public class SistemaCargaController {

    private final CargaService cargaService;
    private final OrdenService ordenService;

    @PostMapping("/{numeroOrden}/habilitar-carga")
    @Operation(summary = "Punto 3.1 - Habilitar carga (password -> preset)",
            description = "Valida que la orden exista, este en estado 2 y la contrasena sea correcta. "
                    + "Devuelve el preset para iniciar la carga. No cambia el estado.")
    @ApiResponse(responseCode = "200", description = "Contrasena correcta; se devuelve el preset")
    @ApiResponse(responseCode = "400", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Contrasena incorrecta",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La orden no esta en estado 2",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public HabilitacionCargaResponse habilitarCarga(@PathVariable Long numeroOrden,
                                                    @Valid @RequestBody HabilitacionCargaRequest request) {
        return cargaService.habilitarCarga(numeroOrden, request);
    }

    @PostMapping("/{numeroOrden}/detalle")
    @Operation(summary = "Punto 3.3 - Recibir dato de carga en tiempo real",
            description = "Se descarta (aceptado=false) si caudal <= 0, masa <= 0, masa menor a la anterior "
                    + "o densidad fuera de (0, 1]. Si es valido actualiza la cabecera de la orden y, segun la "
                    + "frecuencia configurada, se guarda como detalle. Solo para ordenes en estado 2.")
    @ApiResponse(responseCode = "200", description = "Dato procesado (ver aceptado / almacenado / motivoDescarte)")
    @ApiResponse(responseCode = "400", description = "Faltan valores o tienen formato invalido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La orden no esta en estado 2 (por ejemplo, ya fue cerrada)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DetalleCargaResultado detalle(@PathVariable Long numeroOrden, @Valid @RequestBody DetalleCargaRequest request) {
        return cargaService.recibirDetalle(numeroOrden, request);
    }

    @PostMapping("/{numeroOrden}/cierre")
    @Operation(summary = "Punto 4 - Cerrar la orden",
            description = "Pasa la orden a estado 3: Cerrada para carga. A partir de aca se rechazan nuevos datos de "
                    + "detalle. El body es opcional (observaciones para la auditoria). Solo para ordenes en estado 2.")
    @ApiResponse(responseCode = "200", description = "Orden cerrada")
    @ApiResponse(responseCode = "404", description = "La orden no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La orden no esta en estado 2",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public OrdenResponse cerrar(@PathVariable Long numeroOrden,
                                @Valid @RequestBody(required = false) CierreRequest request) {
        return ordenService.cerrar(numeroOrden, request);
    }
}
