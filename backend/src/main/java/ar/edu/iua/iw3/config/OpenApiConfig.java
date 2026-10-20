package ar.edu.iua.iw3.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cabecera de Swagger UI y el boton Authorize. El detalle de cada endpoint sale de los controllers. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                .title("IW3 - Ordenes de carga de gas licuado")
                .version("v1")
                .description("""
                        APIs de integracion con SAP, TMS (balanza) y el sistema de carga.
                        Estados: 1 Pendiente de pesaje inicial -> 2 Con pesaje inicial -> 3 Cerrada para carga -> 4 Finalizada.
                        Primero hacer POST /api/v1/auth/login y pegar el token en Authorize."""));
    }
}
