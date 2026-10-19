package ar.edu.iua.iw3.dto;

import ar.edu.iua.iw3.model.Alarma;
import ar.edu.iua.iw3.model.EstadoAlarma;

import java.time.LocalDateTime;

public record AlarmaResponse(
        Long id,
        Long numeroOrden,
        Double temperatura,
        Double umbral,
        LocalDateTime fechaHora,
        EstadoAlarma estado,
        String usuarioAceptacion,
        LocalDateTime fechaAceptacion,
        String observacion) {

    public static AlarmaResponse desde(Alarma a) {
        return new AlarmaResponse(a.getId(), a.getOrden().getNumeroOrden(), a.getTemperatura(), a.getUmbral(),
                a.getFechaHora(), a.getEstado(), a.getUsuarioAceptacion(), a.getFechaAceptacion(), a.getObservacion());
    }
}
