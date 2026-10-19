package ar.edu.iua.iw3.repository;

import ar.edu.iua.iw3.dto.PromediosCarga;
import ar.edu.iua.iw3.model.DetalleCarga;
import ar.edu.iua.iw3.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DetalleCargaRepository extends JpaRepository<DetalleCarga, Long> {

    List<DetalleCarga> findByOrdenOrderByFechaHoraAsc(Orden orden);

    /** Ultimo detalle guardado: se usa para saber si ya paso el tiempo de la frecuencia de almacenamiento. */
    Optional<DetalleCarga> findTopByOrdenOrderByFechaHoraDesc(Orden orden);

    /** Promedios para la conciliacion, calculados por la base sobre los datos almacenados. */
    @Query("""
            select new ar.edu.iua.iw3.dto.PromediosCarga(avg(d.temperatura), avg(d.densidad), avg(d.caudal))
            from DetalleCarga d where d.orden = :orden""")
    PromediosCarga calcularPromedios(Orden orden);
}
