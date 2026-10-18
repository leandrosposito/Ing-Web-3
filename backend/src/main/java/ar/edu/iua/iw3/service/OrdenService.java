package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.*;
import ar.edu.iua.iw3.exception.ConflictoException;
import ar.edu.iua.iw3.exception.EstadoInvalidoException;
import ar.edu.iua.iw3.exception.RecursoNoEncontradoException;
import ar.edu.iua.iw3.mapper.OrdenMapper;
import ar.edu.iua.iw3.model.CambioEstado;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.repository.CambioEstadoRepository;
import ar.edu.iua.iw3.repository.OrdenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Logica de negocio de la orden y sus cambios de estado.
 * Todos los cambios de estado pasan por {@link #cambiarEstado}, que valida la transicion y la audita.
 */
@Service
@RequiredArgsConstructor
public class OrdenService {

    /** Usuario/proceso que queda registrado en la auditoria (en el final se reemplaza por el usuario autenticado). */
    public static final String PROCESO_SAP = "SAP";
    public static final String PROCESO_TMS = "TMS";
    public static final String PROCESO_SISTEMA_CARGA = "SISTEMA_CARGA";

    private final OrdenRepository ordenRepository;
    private final CambioEstadoRepository cambioEstadoRepository;
    private final SincronizacionService sincronizacionService;
    private final SecureRandom random = new SecureRandom();

    // ------------------------------------------------------------------ Punto 1

    /**
     * Alta de la orden enviada por SAP. Sincroniza las entidades asociadas y crea la orden en estado 1.
     * Es una sola transaccion: si algo falla no queda ni la orden ni los maestros a medio guardar.
     */
    @Transactional
    public OrdenResponse crear(OrdenSapRequest req) {
        if (ordenRepository.existsByNumeroOrden(req.numeroOrden())) {
            throw new ConflictoException("Ya existe una orden con numero " + req.numeroOrden());
        }

        Orden orden = new Orden();
        orden.setNumeroOrden(req.numeroOrden());
        orden.setCamion(sincronizacionService.sincronizarCamion(req.camion()));
        orden.setChofer(sincronizacionService.sincronizarChofer(req.chofer()));
        orden.setCliente(sincronizacionService.sincronizarCliente(req.cliente()));
        orden.setProducto(sincronizacionService.sincronizarProducto(req.producto()));
        orden.setFechaPrevistaCarga(req.fechaPrevistaCarga());
        orden.setPreset(req.preset());
        orden.setFechaRecepcion(LocalDateTime.now());
        orden.setEstado(EstadoOrden.PENDIENTE_PESAJE_INICIAL);
        ordenRepository.save(orden);

        // El alta tambien queda en la auditoria (estado anterior = null)
        cambioEstadoRepository.save(new CambioEstado(orden, null, EstadoOrden.PENDIENTE_PESAJE_INICIAL,
                PROCESO_SAP, "Orden recibida desde SAP"));

        return OrdenMapper.toResponse(orden);
    }

    // ------------------------------------------------------------------ Punto 2

    /** Registra la tara, genera la contrasena de activacion y pasa la orden a estado 2. */
    @Transactional
    public PesajeInicialResponse registrarPesajeInicial(Long numeroOrden, PesajeRequest req) {
        Orden orden = obtenerParaActualizar(numeroOrden);
        cambiarEstado(orden, EstadoOrden.CON_PESAJE_INICIAL, PROCESO_TMS, req.observaciones());

        orden.setPesajeInicial(req.pesaje());
        orden.setFechaPesajeInicial(LocalDateTime.now());
        orden.setPassword(generarPassword());

        return new PesajeInicialResponse(orden.getNumeroOrden(), orden.getEstado(), orden.getPesajeInicial(),
                orden.getFechaPesajeInicial(), orden.getPassword());
    }

    /** Entero aleatorio de 5 digitos (10000 a 99999). SecureRandom porque es una credencial. */
    private Integer generarPassword() {
        return 10000 + random.nextInt(90000);
    }

    // ------------------------------------------------------------------ Consultas

    @Transactional(readOnly = true)
    public OrdenResponse buscar(Long numeroOrden) {
        return OrdenMapper.toResponse(obtener(numeroOrden));
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listar(EstadoOrden estado) {
        List<Orden> ordenes = (estado == null)
                ? ordenRepository.findAllByOrderByNumeroOrdenAsc()
                : ordenRepository.findByEstadoOrderByNumeroOrdenAsc(estado);
        return ordenes.stream().map(OrdenMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CambioEstadoResponse> historialEstados(Long numeroOrden) {
        Orden orden = obtener(numeroOrden);
        return cambioEstadoRepository.findByOrdenOrderByFechaHoraAscIdAsc(orden).stream()
                .map(OrdenMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------ Helpers compartidos

    /** Busca la orden o lanza 404. */
    public Orden obtener(Long numeroOrden) {
        return ordenRepository.findByNumeroOrden(numeroOrden)
                .orElseThrow(() -> noEncontrada(numeroOrden));
    }

    /** Busca la orden bloqueando la fila hasta el fin de la transaccion (para operaciones que la modifican). */
    public Orden obtenerParaActualizar(Long numeroOrden) {
        return ordenRepository.findByNumeroOrdenParaActualizar(numeroOrden)
                .orElseThrow(() -> noEncontrada(numeroOrden));
    }

    /**
     * Unico punto donde cambia el estado de una orden: valida que sea el estado siguiente
     * y registra el cambio en la auditoria (dentro de la misma transaccion del que lo llama).
     */
    void cambiarEstado(Orden orden, EstadoOrden nuevo, String usuario, String observaciones) {
        EstadoOrden actual = orden.getEstado();
        if (!actual.puedePasarA(nuevo)) {
            throw new EstadoInvalidoException("La orden %d esta en estado %d (%s) y no puede pasar a %d (%s)"
                    .formatted(orden.getNumeroOrden(), actual.getNumero(), actual.getDescripcion(),
                            nuevo.getNumero(), nuevo.getDescripcion()));
        }
        orden.setEstado(nuevo);
        cambioEstadoRepository.save(new CambioEstado(orden, actual, nuevo, usuario, observaciones));
    }

    private RecursoNoEncontradoException noEncontrada(Long numeroOrden) {
        return new RecursoNoEncontradoException("No existe la orden " + numeroOrden);
    }
}
