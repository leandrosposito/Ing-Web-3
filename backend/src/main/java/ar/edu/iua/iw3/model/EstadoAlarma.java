package ar.edu.iua.iw3.model;

public enum EstadoAlarma {
    /** Se envio el mail y falta que un usuario autorizado la acepte. Mientras esta asi, no se manda otro mail de la orden. */
    PENDIENTE,
    ACEPTADA
}
