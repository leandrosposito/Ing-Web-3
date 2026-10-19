package ar.edu.iua.iw3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Registro historico de la carga. Se guarda segun la frecuencia configurada, no necesariamente todos los datos recibidos. */
@Entity
@Table(name = "detalle_carga")
@Getter
@Setter
@NoArgsConstructor
public class DetalleCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id")
    private Orden orden;

    /** kg */
    @Column(nullable = false)
    private Double masaAcumulada;

    @Column(nullable = false)
    private Double densidad;

    /** grados C */
    @Column(nullable = false)
    private Double temperatura;

    /** kg/h */
    @Column(nullable = false)
    private Double caudal;

    @Column(nullable = false)
    private LocalDateTime fechaHora;
}
