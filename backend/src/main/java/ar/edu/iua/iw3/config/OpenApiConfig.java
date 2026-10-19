package ar.edu.iua.iw3.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos generales que aparecen en la cabecera de Swagger UI.
 * El detalle de cada endpoint lo genera springdoc a partir de los controllers.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                // Boton "Authorize" de Swagger UI: se pega el token que devuelve /api/v1/auth/login
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                .title("IW3 - Ordenes de carga de gas licuado")
                .version("v1")
                .description("""
                        APIs de integracion con SAP, TMS (balanza) y el sistema de carga.

                        Ciclo de una orden: 1 Pendiente de pesaje inicial -> 2 Con pesaje inicial registrado
                        -> 3 Cerrada para carga -> 4 Finalizada. Los estados no pueden retroceder.

                        Seguridad: primero hacer POST /api/v1/auth/login y usar el token con el boton Authorize.
                        Roles: SAP (alta de orden), TMS (pesajes), SISTEMA_CARGA (habilitar, detalle y cierre),
                        OPERADOR (consultas y alarmas) y ADMIN (todo).

                        Errores: 400 datos invalidos, 401 sin token o password incorrecta, 403 rol sin permiso,
                        404 orden inexistente, 409 la operacion no corresponde al estado actual de la orden
                        (o numero de orden duplicado)."""));
    }
}
