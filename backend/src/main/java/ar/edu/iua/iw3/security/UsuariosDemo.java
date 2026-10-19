package ar.edu.iua.iw3.security;

import ar.edu.iua.iw3.model.Rol;
import ar.edu.iua.iw3.model.Usuario;
import ar.edu.iua.iw3.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, si no hay usuarios, crea uno por cada rol (sap, tms, carga, operador, admin).
 * Todos con la contrasena de la variable PASSWORD_DEMO (por defecto "demo1234", solo para desarrollo).
 */
@Component
@RequiredArgsConstructor
public class UsuariosDemo implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seguridad.password-demo}")
    private String passwordDemo;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        crear("sap", Rol.SAP);
        crear("tms", Rol.TMS);
        crear("carga", Rol.SISTEMA_CARGA);
        crear("operador", Rol.OPERADOR);
        crear("admin", Rol.ADMIN);
    }

    private void crear(String username, Rol rol) {
        usuarioRepository.save(new Usuario(username, passwordEncoder.encode(passwordDemo), rol));
    }
}
