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
 * Consola de H2 (/h2-console), solo con el perfil "h2". Se abre desde el navegador sin JWT y se dibuja con frames,
 * por eso tiene su propia cadena de filtros (sin CSRF y con frameOptions sameOrigin). Sin el perfil no existe.
 */
@Configuration
@Profile("h2")
public class H2ConsoleSecurityConfig {

    @Bean
    @Order(1) // antes que la cadena principal
    public SecurityFilterChain filtrosH2Console(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        return http.build();
    }
}
