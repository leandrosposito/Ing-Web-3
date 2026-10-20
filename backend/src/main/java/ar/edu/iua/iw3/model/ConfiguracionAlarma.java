package ar.edu.iua.iw3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tabla de una sola fila (id fijo = 1). */
@Entity
@Table(name = "configuracion_alarma")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionAlarma {

    public static final long ID = 1L;

    @Id
    private Long id = ID;

    @Column(nullable = false)
    private Double temperaturaUmbral;

    /** Mails separados por coma. */
    @Column(nullable = false, length = 1000)
    private String destinatarios;

    @Column(length = 50)
    private String actualizadoPor;

    private LocalDateTime fechaActualizacion;

    public ConfiguracionAlarma(double temperaturaUmbral, String destinatarios) {
        this.temperaturaUmbral = temperaturaUmbral;
        this.destinatarios = destinatarios;
    }
}
