package ar.edu.iua.iw3.dto;

import jakarta.validation.constraints.Size;

/** La observacion es opcional: el usuario y la fecha/hora se toman del token y del reloj del servidor. */
public record AceptarAlarmaRequest(
        @Size(max = 500) String observacion) {
}
