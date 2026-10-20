package ar.edu.iua.iw3.mapper;

import ar.edu.iua.iw3.dto.CambioEstadoResponse;
import ar.edu.iua.iw3.dto.CamionResponse;
import ar.edu.iua.iw3.dto.ChoferDto;
import ar.edu.iua.iw3.dto.CisternaDto;
import ar.edu.iua.iw3.dto.ClienteDto;
import ar.edu.iua.iw3.dto.DetalleCargaResponse;
import ar.edu.iua.iw3.dto.OrdenResponse;
import ar.edu.iua.iw3.dto.ProductoDto;
import ar.edu.iua.iw3.model.CambioEstado;
import ar.edu.iua.iw3.model.Camion;
import ar.edu.iua.iw3.model.Chofer;
import ar.edu.iua.iw3.model.Cliente;
import ar.edu.iua.iw3.model.DetalleCarga;
import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.model.Producto;

/**
 * Convierte entidades a DTOs de respuesta. Lo hacemos a mano (sin librerias) porque son pocas clases
 * y asi queda explicito que campos se exponen (por ejemplo, la password NO sale en OrdenResponse).
 */
public final class OrdenMapper {

    private OrdenMapper() {
    }

    public static OrdenResponse toResponse(Orden o) {
        return new OrdenResponse(
                o.getNumeroOrden(),
                o.getEstado(),
                o.getEstado().getNumero(),
                o.getEstado().getDescripcion(),
                toResponse(o.getCamion()),
                toDto(o.getChofer()),
                toDto(o.getCliente()),
                toDto(o.getProducto()),
                o.getFechaPrevistaCarga(),
                o.getPreset(),
                o.getFechaRecepcion(),
                o.getPesajeInicial(),
                o.getFechaPesajeInicial(),
                o.getFechaInicioCarga(),
                o.getFechaFinCarga(),
                o.getUltimaMasaAcumulada(),
                o.getUltimaDensidad(),
                o.getUltimaTemperatura(),
                o.getUltimoCaudal(),
                o.getFechaUltimoDato(),
                o.getPesajeFinal(),
                o.getFechaPesajeFinal());
    }

    public static CamionResponse toResponse(Camion c) {
        return new CamionResponse(c.getCodigoExterno(), c.getPatente(), c.getDescripcion(),
                c.getCisternas().stream().map(ci -> new CisternaDto(ci.getNumero(), ci.getCapacidadLitros())).toList(),
                c.getCapacidadTotalLitros());
    }

    public static ChoferDto toDto(Chofer c) {
        return new ChoferDto(c.getCodigoExterno(), c.getNombre(), c.getApellido(), c.getDocumento());
    }

    public static ClienteDto toDto(Cliente c) {
        return new ClienteDto(c.getCodigoExterno(), c.getRazonSocial(), c.getContacto());
    }

    public static ProductoDto toDto(Producto p) {
        return new ProductoDto(p.getCodigoExterno(), p.getNombre(), p.getDescripcion());
    }

    public static CambioEstadoResponse toResponse(CambioEstado c) {
        return new CambioEstadoResponse(c.getEstadoAnterior(), c.getEstadoNuevo(), c.getFechaHora(),
                c.getUsuario(), c.getObservaciones());
    }

    public static DetalleCargaResponse toResponse(DetalleCarga d) {
        return new DetalleCargaResponse(d.getMasaAcumulada(), d.getDensidad(), d.getTemperatura(), d.getCaudal(),
                d.getFechaHora());
    }
}
