package ar.edu.iua.iw3.exception;

/**
 * La operacion no corresponde al estado actual de la orden (ej: mandar detalle a una orden cerrada).
 * Se traduce a HTTP 409 Conflict: el request esta bien formado pero choca con el estado del recurso.
 */
public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
