package ar.edu.iua.iw3.security;

import ar.edu.iua.iw3.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/** Quien puede llamar a cada endpoint. Se leen de arriba hacia abajo y gana la primera regla que coincide. */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                // API stateless con token en el header: no hay cookies ni sesion, asi que no aplica CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Publico: login y documentacion
                        .requestMatchers("/api/v1/auth/login", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Cada sistema externo solo llama a lo suyo (ADMIN puede todo)
                        .requestMatchers(HttpMethod.POST, "/api/v1/ordenes").hasAnyRole("SAP", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ordenes/*/pesaje-inicial", "/api/v1/ordenes/*/pesaje-final")
                        .hasAnyRole("TMS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ordenes/*/habilitar-carga", "/api/v1/ordenes/*/detalle",
                                "/api/v1/ordenes/*/cierre").hasAnyRole("SISTEMA_CARGA", "ADMIN")
                        // Configurar las alarmas (umbral y destinatarios): solo ADMIN
                        .requestMatchers("/api/v1/alarmas/configuracion").hasRole("ADMIN")
                        // Aceptar una alarma: solo un usuario autorizado (el listado lo ve cualquiera logueado)
                        .requestMatchers(HttpMethod.POST, "/api/v1/alarmas/*/aceptar").hasAnyRole("OPERADOR", "ADMIN")
                        // El resto (consultas, conciliacion, etc.) lo puede usar cualquier usuario logueado
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                escribirError(res, 401, "Unauthorized", "Falta el token o es invalido o esta vencido", req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) ->
                                escribirError(res, 403, "Forbidden", "Tu rol no tiene permiso para este endpoint", req.getRequestURI())))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Mismo ErrorResponse que el GlobalExceptionHandler (estos errores ocurren antes de llegar a un controller). */
    private void escribirError(HttpServletResponse res, int status, String error, String mensaje, String path)
            throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        ErrorResponse cuerpo = new ErrorResponse(LocalDateTime.now(), status, error, mensaje, path, List.of());
        res.getWriter().write(objectMapper.writeValueAsString(cuerpo));
    }
}
