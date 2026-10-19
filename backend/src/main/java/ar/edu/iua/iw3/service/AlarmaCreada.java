package ar.edu.iua.iw3.service;

/** Evento que se publica cuando se crea una alarma. Lo escucha NotificadorAlarmas para mandar el mail. */
public record AlarmaCreada(Long alarmaId, Long numeroOrden, double temperatura, double umbral) {
}
