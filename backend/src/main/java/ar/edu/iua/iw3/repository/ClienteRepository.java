package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCodigoExterno(String codigoExterno);
}
