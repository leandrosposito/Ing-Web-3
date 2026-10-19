package ar.edu.iua.iw3.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuario que consume las APIs. Tiene un unico rol (alcanza para este sistema). */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Hash BCrypt: la contrasena en texto plano nunca se guarda. */
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    public Usuario(String username, String passwordHash, Rol rol) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }
}
