package ar.edu.iua.iw3.dto;

import ar.edu.iua.iw3.model.EstadoOrden;

import java.time.LocalDateTime;

/** Punto 2: se devuelve la contrasena de activacion generada (unico lugar donde se expone). */
public record PesajeInicialResponse(Long numeroOrden, EstadoOrden estado, Double pesajeInicial,
                                    LocalDateTime fechaPesajeInicial, Integer password) {
}
