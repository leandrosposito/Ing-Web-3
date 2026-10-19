package ar.edu.iua.iw3.controller;

import ar.edu.iua.iw3.dto.AceptarAlarmaRequest;
import ar.edu.iua.iw3.dto.AlarmaResponse;
import ar.edu.iua.iw3.dto.ErrorResponse;
import ar.edu.iua.iw3.model.EstadoAlarma;
import ar.edu.iua.iw3.service.AlarmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alarmas")
@RequiredArgsConstructor
@Tag(name = "6. Alarmas", description = "Alarmas de temperatura: listado y aceptacion")
public class AlarmaController {

    private final AlarmaService alarmaService;

    @GetMapping
    @Operation(summary = "Listar alarmas", description = "Las mas recientes primero. Opcionalmente filtradas por estado (PENDIENTE o ACEPTADA).")
    public List<AlarmaResponse> listar(
            @Parameter(description = "Filtro opcional por estado") @RequestParam(required = false) EstadoAlarma estado) {
        return alarmaService.listar(estado);
    }

    @PostMapping("/{id}/aceptar")
    @Operation(summary = "Aceptar una alarma",
            description = "Registra el usuario autenticado, la fecha/hora y la observacion. Solo roles OPERADOR y ADMIN. "
                    + "Despues de aceptarla, un nuevo dato sobre el umbral vuelve a generar alarma y mail.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "403", description = "El rol no puede aceptar alarmas",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La alarma no existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La alarma ya estaba aceptada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AlarmaResponse aceptar(@PathVariable Long id, @Valid @RequestBody(required = false) AceptarAlarmaRequest req) {
        return alarmaService.aceptar(id, req);
    }
}
