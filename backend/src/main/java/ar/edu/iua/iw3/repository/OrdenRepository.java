package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.Orden;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByNumeroOrden(Long numeroOrden);

    boolean existsByNumeroOrden(Long numeroOrden);

    /** El "join fetch" trae camion, chofer, cliente y producto en la misma consulta (evita el N+1). Estado null = todas. */
    @Query("""
            select o from Orden o
            join fetch o.camion join fetch o.chofer join fetch o.cliente join fetch o.producto
            where (:estado is null or o.estado = :estado)
            order by o.numeroOrden asc""")
    List<Orden> listar(EstadoOrden estado);

    /**
     * Consulta del monitoreo: el estado es opcional (null = todos) y la patente filtra por "contiene" sin
     * distinguir mayusculas (texto vacio = todas). El orden llega en el parametro Sort.
     * Con "join fetch" trae las entidades asociadas en la misma consulta (evita el N+1).
     */
    @Query("""
            select o from Orden o
            join fetch o.camion c join fetch o.chofer join fetch o.cliente join fetch o.producto
            where (:estado is null or o.estado = :estado)
              and lower(c.patente) like lower(concat('%', :patente, '%'))""")
    List<Orden> buscarParaMonitoreo(EstadoOrden estado, String patente, Sort sort);

    /**
     * Igual que findByNumeroOrden pero bloquea la fila (SELECT ... FOR UPDATE) hasta que termine la transaccion.
     * Lo usamos en las operaciones que modifican la orden: si llegan dos datos de carga a la vez,
     * o un dato y el cierre, se procesan de a uno y no se pisan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orden o where o.numeroOrden = :numeroOrden")
    Optional<Orden> findByNumeroOrdenParaActualizar(Long numeroOrden);
}
