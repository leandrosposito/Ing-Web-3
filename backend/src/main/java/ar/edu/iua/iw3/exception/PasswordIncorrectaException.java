package ar.edu.iua.iw3.exception;

/** La contrasena de activacion no coincide con la de la orden. Se traduce a HTTP 401. */
public class PasswordIncorrectaException extends RuntimeException {
    public PasswordIncorrectaException(String mensaje) {
        super(mensaje);
    }
}
