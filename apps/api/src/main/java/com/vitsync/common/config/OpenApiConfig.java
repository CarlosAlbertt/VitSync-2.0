package com.vitsync.common.config;

import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Datos generales del contrato OpenAPI.
 *
 * <p>springdoc genera la especificación leyendo los controladores y sus anotaciones; este
 * bean le añade lo que no se puede deducir del código: título, versión, servidor y cómo
 * se autentica la API. El resultado se sirve en /v3/api-docs y se ve en /swagger-ui.html.
 */
@Configuration
class OpenApiConfig {

    // Nombre interno del esquema de seguridad; se referencia dos veces más abajo.
    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI vitSyncOpenApi(){
        return new OpenAPI()
                .info(new Info().title("VitSync API").version("1.0").description("Errors follow RFC 9457 with the extensions `code` and `traceId`."))
                    // URL relativa: la spec versionada no depende del host donde se genere
                .servers(List.of(new Server().url("/")))
                // Declara el esquema: token JWT en la cabecera Authorization: Bearer <token>.
                // Con esto, Swagger UI muestra el botón "Authorize" para pegar el token.
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()

                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                // Lo aplica a todas las operaciones por defecto. Los endpoints públicos
                // (registro, login) lo anularán uno a uno cuando existan.
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
        }
    }
