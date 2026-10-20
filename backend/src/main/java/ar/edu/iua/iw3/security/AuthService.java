package ar.edu.iua.iw3.security;

import ar.edu.iua.iw3.dto.LoginRequest;
import ar.edu.iua.iw3.dto.LoginResponse;
import ar.edu.iua.iw3.model.Usuario;
import ar.edu.iua.iw3.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Mismo error para usuario inexistente y clave mala, para no revelar cual fallo. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByUsername(req.username())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Usuario o contrasena incorrectos"));
        String token = jwtService.generar(usuario.getUsername(), usuario.getRol().name());
        return new LoginResponse(token, usuario.getUsername(), usuario.getRol());
    }
}
