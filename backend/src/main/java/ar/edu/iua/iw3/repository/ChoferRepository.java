package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.model.Chofer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChoferRepository extends JpaRepository<Chofer, Long> {
    Optional<Chofer> findByCodigoExterno(String codigoExterno);
}
