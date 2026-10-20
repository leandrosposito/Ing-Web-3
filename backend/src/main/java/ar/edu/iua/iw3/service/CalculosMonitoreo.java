package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.model.EstadoOrden;

import java.time.Duration;
import java.time.LocalDateTime;

/** Calculos del monitoreo, funciones puras (sin base de datos). */
public final class CalculosMonitoreo {

    private CalculosMonitoreo() {
    }

    /** ETA en horas = (preset - masa) / caudal. Solo en estado 2 y con caudal valido; si ya llego al preset, 0. */
    public static Double etaHoras(EstadoOrden estado, Double preset, Double masaAcumulada, Double caudal) {
        if (estado != EstadoOrden.CON_PESAJE_INICIAL || preset == null || masaAcumulada == null
                || caudal == null || caudal <= 0) {
            return null;
        }
        return Math.max(preset - masaAcumulada, 0) / caudal;
    }

    /** Segundos desde el primer dato valido. Solo en estado 2 y si ya empezo a cargar. */
    public static Long tiempoTranscurridoSegundos(EstadoOrden estado, LocalDateTime inicioCarga, LocalDateTime ahora) {
        if (estado != EstadoOrden.CON_PESAJE_INICIAL || inicioCarga == null) {
            return null;
        }
        return Duration.between(inicioCarga, ahora).toSeconds();
    }
}
