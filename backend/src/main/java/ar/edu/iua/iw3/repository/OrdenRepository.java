package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.Orden;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByNumeroOrden(Long numeroOrden);

    boolean existsByNumeroOrden(Long numeroOrden);

    List<Orden> findByEstadoOrderByNumeroOrdenAsc(EstadoOrden estado);

    List<Orden> findAllByOrderByNumeroOrdenAsc();

    /**
     * Igual que findByNumeroOrden pero bloquea la fila (SELECT ... FOR UPDATE) hasta que termine la transaccion.
     * Lo usamos en las operaciones que modifican la orden: si llegan dos datos de carga a la vez,
     * o un dato y el cierre, se procesan de a uno y no se pisan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orden o where o.numeroOrden = :numeroOrden")
    Optional<Orden> findByNumeroOrdenParaActualizar(Long numeroOrden);
}
