package ar.edu.iua.iw3.exception;

/** Conflicto con datos existentes (ej: numero de orden duplicado). Se traduce a HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
