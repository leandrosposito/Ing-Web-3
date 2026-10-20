package ar.edu.iua.iw3.service;

import ar.edu.iua.iw3.dto.CamionDto;
import ar.edu.iua.iw3.dto.ChoferDto;
import ar.edu.iua.iw3.dto.ClienteDto;
import ar.edu.iua.iw3.dto.ProductoDto;
import ar.edu.iua.iw3.model.Camion;
import ar.edu.iua.iw3.model.Chofer;
import ar.edu.iua.iw3.model.Cisterna;
import ar.edu.iua.iw3.model.Cliente;
import ar.edu.iua.iw3.model.Producto;
import ar.edu.iua.iw3.repository.CamionRepository;
import ar.edu.iua.iw3.repository.ChoferRepository;
import ar.edu.iua.iw3.repository.ClienteRepository;
import ar.edu.iua.iw3.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Entidades de SAP por codigo externo (upsert): si ya existe se actualiza, si no se crea. */
@Service
@RequiredArgsConstructor
public class SincronizacionService {

    private final CamionRepository camionRepository;
    private final ChoferRepository choferRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

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

    public Chofer sincronizarChofer(ChoferDto dto) {
        Chofer chofer = choferRepository.findByCodigoExterno(dto.codigoExterno()).orElseGet(Chofer::new);
        chofer.setCodigoExterno(dto.codigoExterno());
        chofer.setNombre(dto.nombre());
        chofer.setApellido(dto.apellido());
        chofer.setDocumento(dto.documento());
        return choferRepository.save(chofer);
    }

    public Cliente sincronizarCliente(ClienteDto dto) {
        Cliente cliente = clienteRepository.findByCodigoExterno(dto.codigoExterno()).orElseGet(Cliente::new);
        cliente.setCodigoExterno(dto.codigoExterno());
        cliente.setRazonSocial(dto.razonSocial());
        cliente.setContacto(dto.contacto());
        return clienteRepository.save(cliente);
    }

    public Producto sincronizarProducto(ProductoDto dto) {
        Producto producto = productoRepository.findByCodigoExterno(dto.codigoExterno()).orElseGet(Producto::new);
        producto.setCodigoExterno(dto.codigoExterno());
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        return productoRepository.save(producto);
    }
}
