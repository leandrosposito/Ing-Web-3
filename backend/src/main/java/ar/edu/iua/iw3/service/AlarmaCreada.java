package ar.edu.iua.iw3.service;

import java.util.List;

/**
 * Evento que se publica cuando se crea una alarma. Lo escucha NotificadorAlarmas para mandar el mail.
 * Lleva los destinatarios vigentes en el momento de la alarma.
 */
public record AlarmaCreada(Long alarmaId, Long numeroOrden, double temperatura, double umbral,
                           List<String> destinatarios) {
}
