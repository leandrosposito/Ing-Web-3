package ar.edu.iua.iw3.service;

import java.util.List;

/** Evento que escucha NotificadorAlarmas para mandar el mail, con los destinatarios vigentes en ese momento. */
public record AlarmaCreada(Long alarmaId, Long numeroOrden, double temperatura, double umbral,
                           List<String> destinatarios) {
}
