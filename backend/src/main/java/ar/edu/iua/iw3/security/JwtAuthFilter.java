package ar.edu.iua.iw3.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Si el request trae "Authorization: Bearer <token>" valido, deja al usuario autenticado; si no, Spring Security responde 401. */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIJO)) {
            jwtService.validar(header.substring(PREFIJO.length())).ifPresent(claims -> {
                // hasRole("X") en SecurityConfig espera la autoridad "ROLE_X"
                var autoridad = new SimpleGrantedAuthority("ROLE_" + claims.get("rol", String.class));
                var autenticacion = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of(autoridad));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            });
        }
        chain.doFilter(request, response);
    }
}
