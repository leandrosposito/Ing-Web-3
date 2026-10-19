package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.CamionDto;
import ar.edu.iua.iw3.dto.ChoferDto;
import ar.edu.iua.iw3.dto.ClienteDto;
import ar.edu.iua.iw3.dto.ProductoDto;
import ar.edu.iua.iw3.model.*;
import ar.edu.iua.iw3.repository.CamionRepository;
import ar.edu.iua.iw3.repository.ChoferRepository;
import ar.edu.iua.iw3.repository.ClienteRepository;
import ar.edu.iua.iw3.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sincroniza las entidades que vienen de SAP usando el codigo externo (upsert):
 * si ya existe una con ese codigo se actualizan sus datos, si no se crea.
 * Los IDs internos nunca se comparten con los sistemas externos.
 */
@Service
@RequiredArgsConstructor
public class SincronizacionService {

    private final CamionRepository camionRepository;
    private final ChoferRepository choferRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    @Transactional
    public Camion sincronizarCamion(CamionDto dto) {
        Camion camion = camionRepository.findByCodigoExterno(dto.codigoExterno()).orElseGet(Camion::new);
        camion.setCodigoExterno(dto.codigoExterno());
        camion.setPatente(dto.patente());
        camion.setDescripcion(dto.descripcion());
        camion.reemplazarCisternas(dto.cisternas().stream()
                .map(c -> new Cisterna(c.numero(), c.capacidadLitros()))
                .toList());
        return camionRepository.save(camion);
    }

    @Transactional
    public Chofer sincronizarChofer(ChoferDto dto) {
        Chofer chofer = choferRepository.findByCodigoExterno(dto.codigoExterno()).orElseGet(Chofer::new);
        chofer.setCodigoExterno(dto.codigoExterno());
        chofer.setNombre(dto.nombre());
        chofer.setApellido(dto.apellido());
        chofer.setDocumento(dto.documento());
        return choferRepository.save(chofer);
    }

    @Transactional
    public Cliente sincronizarCliente(ClienteDto dto) {
        Cliente cliente = clienteRepository.findByCodigoExterno(dto.codigoExterno()).orElseGet(Cliente::new);
        cliente.setCodigoExterno(dto.codigoExterno());
        cliente.setRazonSocial(dto.razonSocial());
        cliente.setContacto(dto.contacto());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Producto sincronizarProducto(ProductoDto dto) {
        Producto producto = productoRepository.findByCodigoExterno(dto.codigoExterno()).orElseGet(Producto::new);
        producto.setCodigoExterno(dto.codigoExterno());
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        return productoRepository.save(producto);
    }
}
