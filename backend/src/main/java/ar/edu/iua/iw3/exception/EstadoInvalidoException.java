package ar.edu.iua.iw3.exception;

/** La operacion no corresponde al estado actual de la orden (se traduce a 409). */
public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
