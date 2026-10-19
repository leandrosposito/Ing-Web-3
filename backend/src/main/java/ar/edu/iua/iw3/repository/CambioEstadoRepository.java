package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.model.CambioEstado;
import ar.edu.iua.iw3.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {
    List<CambioEstado> findByOrdenOrderByFechaHoraAscIdAsc(Orden orden);
}
