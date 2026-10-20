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
import ar.edu.iua.iw3.repository.DetalleCargaRepository;
import ar.edu.iua.iw3.repository.OrdenRepository;
import ar.edu.iua.iw3.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/** Reglas de la orden. Todo cambio de estado pasa por cambiarEstado(), que valida la transicion y la audita. */
@Service
@RequiredArgsConstructor
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final CambioEstadoRepository cambioEstadoRepository;
    private final DetalleCargaRepository detalleCargaRepository;
    private final SincronizacionService sincronizacionService;
    private final SecureRandom random = new SecureRandom();

    /** Es una sola transaccion: si algo falla no queda ni la orden ni los maestros a medio guardar. */
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

        // el alta tambien se audita (estado anterior null)
        cambioEstadoRepository.save(new CambioEstado(orden, null, EstadoOrden.PENDIENTE_PESAJE_INICIAL,
                UsuarioActual.nombre(), "Orden recibida desde SAP"));

        return OrdenMapper.toResponse(orden);
    }

    @Transactional
    public PesajeInicialResponse registrarPesajeInicial(Long numeroOrden, PesajeRequest req) {
        Orden orden = obtenerParaActualizar(numeroOrden);
        cambiarEstado(orden, EstadoOrden.CON_PESAJE_INICIAL, req.observaciones());

        orden.setPesajeInicial(req.pesaje());
        orden.setFechaPesajeInicial(LocalDateTime.now());
        orden.setPassword(generarPassword());

        return new PesajeInicialResponse(orden.getNumeroOrden(), orden.getEstado(), orden.getPesajeInicial(),
                orden.getFechaPesajeInicial(), orden.getPassword());
    }

    /** 5 digitos (10000 a 99999). SecureRandom porque es una credencial. */
    private Integer generarPassword() {
        return 10000 + random.nextInt(90000);
    }

    @Transactional
    public OrdenResponse cerrar(Long numeroOrden, CierreRequest req) {
        Orden orden = obtenerParaActualizar(numeroOrden);
        String observaciones = (req == null) ? null : req.observaciones();
        cambiarEstado(orden, EstadoOrden.CERRADA_PARA_CARGA, observaciones);
        return OrdenMapper.toResponse(orden);
    }

    @Transactional
    public ConciliacionResponse registrarPesajeFinal(Long numeroOrden, PesajeRequest req) {
        Orden orden = obtenerParaActualizar(numeroOrden);
        cambiarEstado(orden, EstadoOrden.FINALIZADA, req.observaciones());
        orden.setPesajeFinal(req.pesaje());
        orden.setFechaPesajeFinal(LocalDateTime.now());
        return calcularConciliacion(orden);
    }

    @Transactional(readOnly = true)
    public ConciliacionResponse obtenerConciliacion(Long numeroOrden) {
        Orden orden = obtener(numeroOrden);
        orden.verificarEstado(EstadoOrden.FINALIZADA);
        return calcularConciliacion(orden);
    }

    /** No se guarda: se calcula con datos que ya estan en la base, asi nunca queda desactualizada. */
    private ConciliacionResponse calcularConciliacion(Orden orden) {
        double productoCargado = orden.getUltimaMasaAcumulada() == null ? 0.0 : orden.getUltimaMasaAcumulada();
        double netoPorBalanza = orden.getPesajeFinal() - orden.getPesajeInicial();
        PromediosCarga promedios = detalleCargaRepository.calcularPromedios(orden);

        return new ConciliacionResponse(
                orden.getNumeroOrden(),
                orden.getPesajeInicial(),
                orden.getPesajeFinal(),
                productoCargado,
                netoPorBalanza,
                netoPorBalanza - productoCargado,
                promedios.temperatura(),
                promedios.densidad(),
                promedios.caudal());
    }

    @Transactional(readOnly = true)
    public OrdenResponse buscar(Long numeroOrden) {
        return OrdenMapper.toResponse(obtener(numeroOrden));
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listar(EstadoOrden estado) {
        return ordenRepository.listar(estado).stream().map(OrdenMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CambioEstadoResponse> historialEstados(Long numeroOrden) {
        Orden orden = obtener(numeroOrden);
        return cambioEstadoRepository.findByOrdenOrderByFechaHoraAscIdAsc(orden).stream()
                .map(OrdenMapper::toResponse)
                .toList();
    }

    public Orden obtener(Long numeroOrden) {
        return ordenRepository.findByNumeroOrden(numeroOrden)
                .orElseThrow(() -> noEncontrada(numeroOrden));
    }

    /** Igual que obtener() pero bloquea la fila hasta el fin de la transaccion (ver OrdenRepository). */
    public Orden obtenerParaActualizar(Long numeroOrden) {
        return ordenRepository.findByNumeroOrdenParaActualizar(numeroOrden)
                .orElseThrow(() -> noEncontrada(numeroOrden));
    }

    /** Valida que sea el estado siguiente y lo registra en la auditoria, en la transaccion del que lo llama. */
    void cambiarEstado(Orden orden, EstadoOrden nuevo, String observaciones) {
        EstadoOrden actual = orden.getEstado();
        if (!actual.puedePasarA(nuevo)) {
            throw new EstadoInvalidoException("La orden %d esta en estado %d (%s) y no puede pasar a %d (%s)"
                    .formatted(orden.getNumeroOrden(), actual.getNumero(), actual.getDescripcion(),
                            nuevo.getNumero(), nuevo.getDescripcion()));
        }
        orden.setEstado(nuevo);
        cambioEstadoRepository.save(new CambioEstado(orden, actual, nuevo, UsuarioActual.nombre(), observaciones));
    }

    private RecursoNoEncontradoException noEncontrada(Long numeroOrden) {
        return new RecursoNoEncontradoException("No existe la orden " + numeroOrden);
    }
}
