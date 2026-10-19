package ar.edu.iua.iw3.exception;

import ar.edu.iua.iw3.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Traduce las excepciones a respuestas HTTP con el mismo formato (ErrorResponse).
 * Asi los controllers y services no se ocupan de armar respuestas de error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return armar(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler({EstadoInvalidoException.class, ConflictoException.class})
    public ResponseEntity<ErrorResponse> conflicto(RuntimeException ex, HttpServletRequest req) {
        return armar(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(PasswordIncorrectaException.class)
    public ResponseEntity<ErrorResponse> passwordIncorrecta(PasswordIncorrectaException ex, HttpServletRequest req) {
        return armar(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, List.of());
    }

    /** Falla de Bean Validation (@NotNull, @Positive, etc.) en el body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .toList();
        return armar(HttpStatus.BAD_REQUEST, "Datos de entrada invalidos", req, detalles);
    }

    /** JSON mal formado o con tipos incorrectos (ej: texto donde va un numero, fecha mal escrita). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return armar(HttpStatus.BAD_REQUEST, "El cuerpo del request no es un JSON valido o tiene tipos incorrectos", req, List.of());
    }

    /** Parametro de la URL con tipo incorrecto (ej: /ordenes/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoParametro(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return armar(HttpStatus.BAD_REQUEST, "Valor invalido para el parametro '" + ex.getName() + "'", req, List.of());
    }

    /** Red de seguridad: dos altas simultaneas con el mismo numero de orden o codigo externo. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violacion de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return armar(HttpStatus.CONFLICT, "El dato ya existe o viola una restriccion de la base", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest req) {
        // Errores propios de Spring MVC (ruta inexistente 404, metodo no soportado 405, etc.) ya traen su status
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return armar(status, status.getReasonPhrase(), req, List.of());
        }
        log.error("Error inesperado en {}", req.getRequestURI(), ex);
        return armar(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, List.of());
    }

    private ResponseEntity<ErrorResponse> armar(HttpStatus status, String mensaje, HttpServletRequest req, List<String> detalles) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensaje, req.getRequestURI(), detalles);
        return ResponseEntity.status(status).body(body);
    }
}
