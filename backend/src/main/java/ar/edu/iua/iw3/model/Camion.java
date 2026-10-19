package ar.edu.iua.iw3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Camion sincronizado desde SAP. Se identifica entre sistemas por su codigo externo. */
@Entity
@Table(name = "camion")
@Getter
@Setter
@NoArgsConstructor
public class Camion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigoExterno;

    @Column(nullable = false, length = 20)
    private String patente;

    private String descripcion;

    /** Cisternado: una fila por cisterna. orphanRemoval borra las que SAP deja de mandar. */
    @OneToMany(mappedBy = "camion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<Cisterna> cisternas = new ArrayList<>();

    /** El total no se guarda: se calcula sumando las cisternas para que nunca quede desactualizado. */
    public double getCapacidadTotalLitros() {
        return cisternas.stream().mapToDouble(Cisterna::getCapacidadLitros).sum();
    }

    public void reemplazarCisternas(List<Cisterna> nuevas) {
        cisternas.clear();
        nuevas.forEach(c -> {
            c.setCamion(this);
            cisternas.add(c);
        });
    }
}
