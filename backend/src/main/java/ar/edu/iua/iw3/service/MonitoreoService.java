package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.MonitoreoOrdenResponse;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Filas de la pantalla de monitoreo: filtra y ordena en la base y agrega el ETA y el tiempo transcurrido. */
@Service
@RequiredArgsConstructor
public class MonitoreoService {

    /** Lista blanca: el valor de la URL se traduce a una propiedad, asi nunca se arma un ORDER BY con texto del cliente. */
    private static final Map<String, String> COLUMNAS_ORDENABLES = Map.of(
            "numeroOrden", "numeroOrden",
            "estado", "estado",
            "patente", "camion.patente",
            "preset", "preset",
            "masa", "ultimaMasaAcumulada",
            "temperatura", "ultimaTemperatura",
            "densidad", "ultimaDensidad",
            "caudal", "ultimoCaudal");

    private final OrdenRepository ordenRepository;

    @Transactional(readOnly = true)
    public List<MonitoreoOrdenResponse> listar(EstadoOrden estado, String patente, String ordenarPor, boolean descendente) {
        String propiedad = COLUMNAS_ORDENABLES.getOrDefault(ordenarPor, "numeroOrden");
        Sort sort = Sort.by(descendente ? Sort.Direction.DESC : Sort.Direction.ASC, propiedad);
        String textoPatente = (patente == null) ? "" : patente.trim();

        LocalDateTime ahora = LocalDateTime.now();
        return ordenRepository.buscarParaMonitoreo(estado, textoPatente, sort).stream()
                .map(o -> aFila(o, ahora))
                .toList();
    }

    private MonitoreoOrdenResponse aFila(Orden o, LocalDateTime ahora) {
        return new MonitoreoOrdenResponse(
                o.getNumeroOrden(),
                o.getEstado(),
                o.getEstado().getNumero(),
                o.getEstado().getDescripcion(),
                o.getCamion().getPatente(),
                o.getPreset(),
                o.getUltimaMasaAcumulada(),
                o.getUltimaTemperatura(),
                o.getUltimaDensidad(),
                o.getUltimoCaudal(),
                o.getFechaUltimoDato(),
                CalculosMonitoreo.etaHoras(o.getEstado(), o.getPreset(), o.getUltimaMasaAcumulada(), o.getUltimoCaudal()),
                CalculosMonitoreo.tiempoTranscurridoSegundos(o.getEstado(), o.getFechaInicioCarga(), ahora));
    }
}
