package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.model.Camion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CamionRepository extends JpaRepository<Camion, Long> {
    Optional<Camion> findByCodigoExterno(String codigoExterno);
}
