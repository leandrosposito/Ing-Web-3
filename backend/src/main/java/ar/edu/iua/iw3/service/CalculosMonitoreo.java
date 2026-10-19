package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.model.EstadoOrden;

import java.time.Duration;
import java.time.LocalDateTime;

/** Calculos de la pantalla de monitoreo. Son funciones puras (sin base de datos), por eso se testean facil. */
public final class CalculosMonitoreo {

    private CalculosMonitoreo() {
    }

    /**
     * ETA de llenado en horas = (preset - masa acumulada) / caudal.
     * Solo tiene sentido mientras se esta cargando (estado 2) y con un caudal valido.
     * Si la masa ya alcanzo o paso el preset, falta 0 horas.
     */
    public static Double etaHoras(EstadoOrden estado, Double preset, Double masaAcumulada, Double caudal) {
        if (estado != EstadoOrden.CON_PESAJE_INICIAL || preset == null || masaAcumulada == null
                || caudal == null || caudal <= 0) {
            return null;
        }
        return Math.max(preset - masaAcumulada, 0) / caudal;
    }

    /** Segundos desde el primer dato valido de carga. Solo para ordenes en estado 2 que ya empezaron a cargar. */
    public static Long tiempoTranscurridoSegundos(EstadoOrden estado, LocalDateTime inicioCarga, LocalDateTime ahora) {
        if (estado != EstadoOrden.CON_PESAJE_INICIAL || inicioCarga == null) {
            return null;
        }
        return Duration.between(inicioCarga, ahora).toSeconds();
    }
}
