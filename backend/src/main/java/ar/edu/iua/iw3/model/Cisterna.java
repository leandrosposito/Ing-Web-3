package ar.edu.iua.iw3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cisterna")
@Getter
@Setter
@NoArgsConstructor
public class Cisterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero de cisterna dentro del camion (1, 2, ...). */
    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private Double capacidadLitros;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id")
    private Camion camion;

    public Cisterna(Integer numero, Double capacidadLitros) {
        this.numero = numero;
        this.capacidadLitros = capacidadLitros;
    }
}
