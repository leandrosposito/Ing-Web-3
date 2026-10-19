package ar.edu.iua.iw3.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
        return new OpenAPI().info(new Info()
                .title("IW3 - Ordenes de carga de gas licuado")
                .version("v1")
                .description("""
                        APIs de integracion con SAP, TMS (balanza) y el sistema de carga.

                        Ciclo de una orden: 1 Pendiente de pesaje inicial -> 2 Con pesaje inicial registrado
                        -> 3 Cerrada para carga -> 4 Finalizada. Los estados no pueden retroceder.

                        Errores: 400 datos invalidos, 401 password incorrecta, 404 orden inexistente,
                        409 la operacion no corresponde al estado actual de la orden (o numero de orden duplicado)."""));
    }
}
