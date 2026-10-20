package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.DetalleCargaRequest;
import ar.edu.iua.iw3.dto.DetalleCargaResponse;
import ar.edu.iua.iw3.dto.DetalleCargaResultado;
import ar.edu.iua.iw3.dto.HabilitacionCargaRequest;
import ar.edu.iua.iw3.dto.HabilitacionCargaResponse;
import ar.edu.iua.iw3.exception.PasswordIncorrectaException;
import ar.edu.iua.iw3.mapper.OrdenMapper;
import ar.edu.iua.iw3.model.DetalleCarga;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.repository.DetalleCargaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Punto 3: habilitacion de la carga y recepcion de datos en tiempo real. */
@Service
public class CargaService {

    private final OrdenService ordenService;
    private final DetalleCargaRepository detalleCargaRepository;
    private final AlarmaService alarmaService;

    /** Cada cuantos segundos se guarda un detalle (0 = todos). */
    private final long frecuenciaAlmacenamientoSegundos;

    public CargaService(OrdenService ordenService, DetalleCargaRepository detalleCargaRepository,
                        AlarmaService alarmaService,
                        @Value("${app.carga.frecuencia-almacenamiento-segundos:0}") long frecuenciaAlmacenamientoSegundos) {
        this.ordenService = ordenService;
        this.detalleCargaRepository = detalleCargaRepository;
        this.alarmaService = alarmaService;
        this.frecuenciaAlmacenamientoSegundos = frecuenciaAlmacenamientoSegundos;
    }

    /** Devuelve el preset solo si la orden esta en estado 2 y la contrasena es correcta. No cambia el estado. */
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
     * Un dato invalido se descarta. Uno valido actualiza siempre la cabecera de la orden
     * y se guarda como detalle solo si paso la frecuencia configurada.
     */
    @Transactional
    public DetalleCargaResultado recibirDetalle(Long numeroOrden, DetalleCargaRequest dato) {
        // bloquea la fila: dos datos simultaneos (o un dato y el cierre) se procesan de a uno
        Orden orden = ordenService.obtenerParaActualizar(numeroOrden);
        orden.verificarEstado(EstadoOrden.CON_PESAJE_INICIAL);

        Optional<String> motivo = ReglasDetalleCarga.motivoDescarte(dato, orden.getUltimaMasaAcumulada());
        if (motivo.isPresent()) {
            return DetalleCargaResultado.descartado(numeroOrden, motivo.get());
        }

        LocalDateTime ahora = LocalDateTime.now();
        actualizarCabecera(orden, dato, ahora);
        alarmaService.evaluar(orden, dato.temperatura());

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
            orden.setFechaInicioCarga(ahora);
        }
        orden.setFechaFinCarga(ahora);
        orden.setUltimaMasaAcumulada(dato.masaAcumulada());
        orden.setUltimaDensidad(dato.densidad());
        orden.setUltimaTemperatura(dato.temperatura());
        orden.setUltimoCaudal(dato.caudal());
        orden.setFechaUltimoDato(ahora);
    }

    private boolean debeAlmacenar(Orden orden, LocalDateTime ahora) {
        LocalDateTime ultimoGuardado = detalleCargaRepository.findTopByOrdenOrderByFechaHoraDesc(orden)
                .map(DetalleCarga::getFechaHora)
                .orElse(null);
        return ReglasAlmacenamiento.debeAlmacenar(ultimoGuardado, ahora, frecuenciaAlmacenamientoSegundos);
    }
}
