package ar.edu.iua.iw3.dto;

/** Punto 3.2: el backend devuelve el preset para que el sistema de carga arranque la bomba. */
public record HabilitacionCargaResponse(Long numeroOrden, Double preset) {
}
