package ar.edu.iua.iw3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Configuracion de las alarmas de temperatura. Es una tabla de UNA sola fila (id fijo = 1).
 * Los valores iniciales salen de application.properties; despues se editan desde el sistema.
 */
@Entity
@Table(name = "configuracion_alarma")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionAlarma {

    public static final long ID = 1L;

    @Id
    private Long id = ID;

    /** Se alarma cuando la temperatura supera este valor (grados). */
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
