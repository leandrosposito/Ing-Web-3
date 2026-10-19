package ar.edu.iua.iw3.dto;

/** Resultado de la consulta de promedios. Los valores son null si la orden no tiene detalle almacenado. */
public record PromediosCarga(Double temperatura, Double densidad, Double caudal) {
}
