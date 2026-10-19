package ar.edu.iua.iw3.model;

import ar.edu.iua.iw3.exception.EstadoInvalidoException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Nucleo del proceso: concentra los datos de todos los eventos del ciclo de carga.
 * El id es interno; los sistemas externos identifican la orden por numeroOrden.
 */
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

    // Entidades asociadas (ManyToOne es EAGER por defecto: siempre las mostramos junto con la orden)
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

    // ---- Punto 1: datos recibidos de SAP ----
    @Column(nullable = false)
    private LocalDateTime fechaPrevistaCarga;

    /** Kilogramos a cargar. */
    @Column(nullable = false)
    private Double preset;

    @Column(nullable = false)
    private LocalDateTime fechaRecepcion;

    // ---- Punto 2: pesaje inicial (tara) ----
    private Double pesajeInicial;
    private LocalDateTime fechaPesajeInicial;

    /** Contrasena de activacion de 5 digitos. */
    private Integer password;

    // ---- Punto 3: carga en tiempo real (cabecera con los ultimos valores validos) ----
    private LocalDateTime fechaInicioCarga;
    private LocalDateTime fechaFinCarga;
    private Double ultimaMasaAcumulada;
    private Double ultimaDensidad;
    private Double ultimaTemperatura;
    private Double ultimoCaudal;
    private LocalDateTime fechaUltimoDato;

    // ---- Punto 5: pesaje final ----
    private Double pesajeFinal;
    private LocalDateTime fechaPesajeFinal;

    /** Para operaciones que no cambian el estado pero solo se permiten en uno (ej: recibir detalle solo en estado 2). */
    public void verificarEstado(EstadoOrden esperado) {
        if (estado != esperado) {
            throw new EstadoInvalidoException("La orden %d esta en estado %d (%s); esta operacion requiere estado %d (%s)"
                    .formatted(numeroOrden, estado.getNumero(), estado.getDescripcion(),
                            esperado.getNumero(), esperado.getDescripcion()));
        }
    }
}
