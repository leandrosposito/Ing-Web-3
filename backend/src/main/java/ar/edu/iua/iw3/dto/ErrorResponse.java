package ar.edu.iua.iw3.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Formato unico de error para todos los endpoints. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        List<String> detalles) {
}
