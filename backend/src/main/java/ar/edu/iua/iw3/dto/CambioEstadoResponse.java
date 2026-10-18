package ar.edu.iua.iw3.dto;

import ar.edu.iua.iw3.model.EstadoOrden;

import java.time.LocalDateTime;

public record CambioEstadoResponse(EstadoOrden estadoAnterior, EstadoOrden estadoNuevo, LocalDateTime fechaHora,
                                   String usuario, String observaciones) {
}
