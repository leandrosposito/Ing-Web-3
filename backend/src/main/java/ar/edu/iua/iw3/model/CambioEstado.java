package ar.edu.iua.iw3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Log de auditoria: un registro por cada cambio de estado de una orden. */
@Entity
@Table(name = "cambio_estado")
@Getter
@Setter
@NoArgsConstructor
public class CambioEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id")
    private Orden orden;

    /** Null en el alta de la orden (no habia estado previo). */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoOrden estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoOrden estadoNuevo;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    /** Usuario autenticado que hizo la transicion (ej: sap, tms, carga). */
    @Column(nullable = false, length = 50)
    private String usuario;

    @Column(length = 500)
    private String observaciones;

    public CambioEstado(Orden orden, EstadoOrden estadoAnterior, EstadoOrden estadoNuevo,
                        String usuario, String observaciones) {
        this.orden = orden;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.observaciones = observaciones;
        this.fechaHora = LocalDateTime.now();
    }
}
