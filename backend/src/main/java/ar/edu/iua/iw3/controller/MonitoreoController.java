package ar.edu.iua.iw3.controller;

import ar.edu.iua.iw3.dto.MonitoreoOrdenResponse;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.service.MonitoreoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monitoreo")
@RequiredArgsConstructor
@Tag(name = "5. Monitoreo", description = "Seguimiento de las ordenes en tiempo real")
public class MonitoreoController {

    private final MonitoreoService monitoreoService;

    @GetMapping
    @Operation(summary = "Monitoreo de ordenes",
            description = "Ultimos valores de carga de cada orden, con ETA de llenado (horas) y tiempo transcurrido desde el "
                    + "inicio de carga (segundos). Ambos calculos solo se informan para ordenes en estado 2. "
                    + "El frontend lo consulta cada pocos segundos.")
    public List<MonitoreoOrdenResponse> monitoreo(
            @Parameter(description = "Filtro por estado") @RequestParam(required = false) EstadoOrden estado,
            @Parameter(description = "Filtro por patente (contiene el texto)") @RequestParam(required = false) String patente,
            @Parameter(description = "numeroOrden, estado, patente, preset, masa, temperatura, densidad o caudal")
            @RequestParam(required = false, defaultValue = "numeroOrden") String ordenarPor,
            @Parameter(description = "asc o desc") @RequestParam(required = false, defaultValue = "asc") String direccion) {
        return monitoreoService.listar(estado, patente, ordenarPor, "desc".equalsIgnoreCase(direccion));
    }
}
