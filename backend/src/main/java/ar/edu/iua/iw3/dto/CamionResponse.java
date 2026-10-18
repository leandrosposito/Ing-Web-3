package ar.edu.iua.iw3.dto;

import java.util.List;

public record CamionResponse(String codigoExterno, String patente, String descripcion,
                             List<CisternaDto> cisternas, double capacidadTotalLitros) {
}
