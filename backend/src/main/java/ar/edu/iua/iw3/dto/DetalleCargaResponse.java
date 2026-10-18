package ar.edu.iua.iw3.dto;

import java.time.LocalDateTime;

/** Registro de detalle almacenado (consulta). */
public record DetalleCargaResponse(Double masaAcumulada, Double densidad, Double temperatura, Double caudal,
                                   LocalDateTime fechaHora) {
}
