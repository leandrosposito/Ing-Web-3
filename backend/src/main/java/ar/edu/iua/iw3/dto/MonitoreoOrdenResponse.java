package ar.edu.iua.iw3.dto;

import ar.edu.iua.iw3.model.EstadoOrden;

import java.time.LocalDateTime;

/** Una fila de la pantalla de monitoreo: los ultimos valores de la orden mas los dos calculos derivados. */
public record MonitoreoOrdenResponse(
        Long numeroOrden,
        EstadoOrden estado,
        int estadoNumero,
        String estadoDescripcion,
        String patente,
        Double preset,
        Double ultimaMasaAcumulada,
        Double ultimaTemperatura,
        Double ultimaDensidad,
        Double ultimoCaudal,
        LocalDateTime fechaUltimoDato,
        /** (preset - masa) / caudal. Null si la orden no esta en estado 2 o todavia no hay caudal valido. */
        Double etaHoras,
        /** Segundos desde el inicio de carga. Null si la orden no esta en estado 2 o todavia no empezo a cargar. */
        Long tiempoTranscurridoSegundos) {
}
