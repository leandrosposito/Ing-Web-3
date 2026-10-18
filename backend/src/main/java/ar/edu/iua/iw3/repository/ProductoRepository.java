package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByCodigoExterno(String codigoExterno);
}
