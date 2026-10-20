package ar.edu.iua.iw3.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Consola web de H2 (/h2-console), SOLO con el perfil "h2" (base en memoria para desarrollo).
 * Sin el perfil esta clase no existe y la consola esta deshabilitada: ahi /h2-console cae en la cadena
 * principal, que exige token, y como la consola no esta habilitada responde 404.
 *
 * La consola no usa JWT (se abre desde el navegador y tiene su propio login de base) y se dibuja con frames,
 * por eso necesita su propia cadena de filtros: sin CSRF y con frameOptions "sameOrigin".
 */
@Configuration
@Profile("h2")
public class H2ConsoleSecurityConfig {

    @Bean
    @Order(1) // antes que la cadena principal, que atiende todo lo demas
    public SecurityFilterChain filtrosH2Console(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }
}
