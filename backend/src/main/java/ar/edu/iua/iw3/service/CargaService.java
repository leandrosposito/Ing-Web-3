package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.*;
import ar.edu.iua.iw3.exception.PasswordIncorrectaException;
import ar.edu.iua.iw3.mapper.OrdenMapper;
import ar.edu.iua.iw3.model.DetalleCarga;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.repository.DetalleCargaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Punto 3: habilitacion de la carga y recepcion de datos en tiempo real. */
@Service
public class CargaService {

    private final OrdenService ordenService;
    private final DetalleCargaRepository detalleCargaRepository;

    /** Cada cuantos segundos se guarda un registro de detalle (0 = todos). Se configura en application.properties. */
    private final long frecuenciaAlmacenamientoSegundos;

    public CargaService(OrdenService ordenService, DetalleCargaRepository detalleCargaRepository,
                        @Value("${app.carga.frecuencia-almacenamiento-segundos:0}") long frecuenciaAlmacenamientoSegundos) {
        this.ordenService = ordenService;
        this.detalleCargaRepository = detalleCargaRepository;
        this.frecuenciaAlmacenamientoSegundos = frecuenciaAlmacenamientoSegundos;
    }

    /**
     * Punto 3.1/3.2: devuelve el preset solo si la orden existe, esta en estado 2 y la contrasena es correcta.
     * No cambia el estado: la orden sigue en 2 mientras se carga.
     */
    @Transactional(readOnly = true)
    public HabilitacionCargaResponse habilitarCarga(Long numeroOrden, HabilitacionCargaRequest req) {
        Orden orden = ordenService.obtener(numeroOrden);
        orden.verificarEstado(EstadoOrden.CON_PESAJE_INICIAL);
        if (!orden.getPassword().equals(req.password())) {
            throw new PasswordIncorrectaException("Contrasena de activacion incorrecta para la orden " + numeroOrden);
        }
        return new HabilitacionCargaResponse(orden.getNumeroOrden(), orden.getPreset());
    }

    /**
     * Punto 3.3: recibe un dato de carga.
     * - Si no es valido se descarta (no toca nada).
     * - Si es valido actualiza siempre la cabecera de la orden con los ultimos valores.
     * - Ademas lo guarda como detalle si ya paso el tiempo de la frecuencia configurada.
     */
    @Transactional
    public DetalleCargaResultado recibirDetalle(Long numeroOrden, DetalleCargaRequest dato) {
        // Bloqueo la fila: dos datos simultaneos (o un dato y el cierre) se procesan de a uno
        Orden orden = ordenService.obtenerParaActualizar(numeroOrden);
        orden.verificarEstado(EstadoOrden.CON_PESAJE_INICIAL);

        Optional<String> motivo = ReglasDetalleCarga.motivoDescarte(dato, orden.getUltimaMasaAcumulada());
        if (motivo.isPresent()) {
            return DetalleCargaResultado.descartado(numeroOrden, motivo.get());
        }

        LocalDateTime ahora = LocalDateTime.now();
        actualizarCabecera(orden, dato, ahora);

        boolean almacenar = debeAlmacenar(orden, ahora);
        if (almacenar) {
            DetalleCarga detalle = new DetalleCarga();
            detalle.setOrden(orden);
            detalle.setMasaAcumulada(dato.masaAcumulada());
            detalle.setDensidad(dato.densidad());
            detalle.setTemperatura(dato.temperatura());
            detalle.setCaudal(dato.caudal());
            detalle.setFechaHora(ahora);
            detalleCargaRepository.save(detalle);
        }
        return DetalleCargaResultado.aceptado(numeroOrden, almacenar);
    }

    @Transactional(readOnly = true)
    public List<DetalleCargaResponse> listarDetalle(Long numeroOrden) {
        Orden orden = ordenService.obtener(numeroOrden);
        return detalleCargaRepository.findByOrdenOrderByFechaHoraAsc(orden).stream()
                .map(OrdenMapper::toResponse)
                .toList();
    }

    private void actualizarCabecera(Orden orden, DetalleCargaRequest dato, LocalDateTime ahora) {
        if (orden.getFechaInicioCarga() == null) {
            orden.setFechaInicioCarga(ahora); // primer dato valido
        }
        orden.setFechaFinCarga(ahora);        // ultimo dato valido (se va pisando)
        orden.setUltimaMasaAcumulada(dato.masaAcumulada());
        orden.setUltimaDensidad(dato.densidad());
        orden.setUltimaTemperatura(dato.temperatura());
        orden.setUltimoCaudal(dato.caudal());
        orden.setFechaUltimoDato(ahora);
    }

    /** Se guarda si la frecuencia es 0, si es el primer detalle, o si desde el ultimo guardado paso el tiempo configurado. */
    private boolean debeAlmacenar(Orden orden, LocalDateTime ahora) {
        if (frecuenciaAlmacenamientoSegundos <= 0) {
            return true;
        }
        return detalleCargaRepository.findTopByOrdenOrderByFechaHoraDesc(orden)
                .map(ultimo -> Duration.between(ultimo.getFechaHora(), ahora).toSeconds() >= frecuenciaAlmacenamientoSegundos)
                .orElse(true);
    }
}
