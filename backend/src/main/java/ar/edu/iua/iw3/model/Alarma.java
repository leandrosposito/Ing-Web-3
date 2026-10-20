package ar.edu.iua.iw3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Alarma por temperatura alta en una orden. Se crea al superar el umbral y queda pendiente hasta que se acepta. */
@Entity
@Table(name = "alarma")
@Getter
@Setter
@NoArgsConstructor
public class Alarma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id")
    private Orden orden;

    /** Temperatura del dato que disparo la alarma. */
    @Column(nullable = false)
    private Double temperatura;

    /** Umbral vigente cuando se disparo (queda guardado aunque despues se cambie la configuracion). */
    @Column(nullable = false)
    private Double umbral;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAlarma estado;

    // se completan al aceptarla
    @Column(length = 50)
    private String usuarioAceptacion;

    private LocalDateTime fechaAceptacion;

    @Column(length = 500)
    private String observacion;

    public Alarma(Orden orden, double temperatura, double umbral) {
        this.orden = orden;
        this.temperatura = temperatura;
        this.umbral = umbral;
        this.fechaHora = LocalDateTime.now();
        this.estado = EstadoAlarma.PENDIENTE;
    }
}
