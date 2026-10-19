package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.model.Alarma;
import ar.edu.iua.iw3.model.EstadoAlarma;
import ar.edu.iua.iw3.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmaRepository extends JpaRepository<Alarma, Long> {

    /** Sirve para no volver a alarmar una orden que ya tiene una alarma sin aceptar. */
    boolean existsByOrdenAndEstado(Orden orden, EstadoAlarma estado);

    List<Alarma> findAllByOrderByFechaHoraDesc();

    List<Alarma> findByEstadoOrderByFechaHoraDesc(EstadoAlarma estado);
}
