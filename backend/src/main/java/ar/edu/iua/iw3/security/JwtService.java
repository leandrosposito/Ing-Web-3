package ar.edu.iua.iw3.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/** Genera y valida los tokens JWT (firmados con HS256). */
@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration expiracion;

    public JwtService(@Value("${app.seguridad.jwt-secret}") String secreto,
                      @Value("${app.seguridad.jwt-expiracion-minutos:120}") long expiracionMinutos) {
        // HS256 exige al menos 32 bytes
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracion = Duration.ofMinutes(expiracionMinutos);
    }

    /** El token lleva usuario y rol, asi no se consulta la base en cada request. */
    public String generar(String username, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracion.toMillis()))
                .signWith(clave)
                .compact();
    }

    /** Devuelve los datos del token, o vacio si esta vencido, adulterado o mal formado. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
