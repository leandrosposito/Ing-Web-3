package ar.edu.iua.iw3.model;

import ar.edu.iua.iw3.exception.EstadoInvalidoException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Los sistemas externos identifican la orden por numeroOrden; el id es interno. */
@Entity
@Table(name = "orden")
@Getter
@Setter
@NoArgsConstructor
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long numeroOrden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoOrden estado;

    // ManyToOne es EAGER por defecto
    @ManyToOne(optional = false)
    @JoinColumn(name = "camion_id")
    private Camion camion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chofer_id")
    private Chofer chofer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    // datos recibidos de SAP
    @Column(nullable = false)
    private LocalDateTime fechaPrevistaCarga;

    /** Kilogramos a cargar. */
    @Column(nullable = false)
    private Double preset;

    @Column(nullable = false)
    private LocalDateTime fechaRecepcion;

    // pesaje inicial (tara)
    private Double pesajeInicial;
    private LocalDateTime fechaPesajeInicial;

    /** Contrasena de activacion de 5 digitos. */
    private Integer password;

    // carga en tiempo real (cabecera con los ultimos valores validos)
    private LocalDateTime fechaInicioCarga;
    private LocalDateTime fechaFinCarga;
    private Double ultimaMasaAcumulada;
    private Double ultimaDensidad;
    private Double ultimaTemperatura;
    private Double ultimoCaudal;
    private LocalDateTime fechaUltimoDato;

    // pesaje final
    private Double pesajeFinal;
    private LocalDateTime fechaPesajeFinal;

    /** Para operaciones que no cambian el estado pero solo valen en uno (ej: recibir detalle, solo en estado 2). */
    public void verificarEstado(EstadoOrden esperado) {
        if (estado != esperado) {
            throw new EstadoInvalidoException("La orden %d esta en estado %d (%s); esta operacion requiere estado %d (%s)"
                    .formatted(numeroOrden, estado.getNumero(), estado.getDescripcion(),
                            esperado.getNumero(), esperado.getDescripcion()));
        }
    }
}
