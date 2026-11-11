package com.vitsync.common.config;

import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * En las respuestas correctas (2xx) todos los campos son obligatorios: Jackson los escribe
     * siempre, y los que no tienen valor salen como {@code null} (y ya están marcados como
     * nullable). springdoc no lo declara, y sin {@code required} el frontend, que genera sus
     * tipos de este contrato, vería cada campo como opcional ({@code firstName?: string}).
     *
     * <p>Solo se recorren los esquemas que cuelgan de una respuesta 2xx: en las peticiones hay
     * campos opcionales de verdad (el teléfono del registro), y {@code ProblemDetail} omite los
     * campos vacíos.
     */
    @Bean
    OpenApiCustomizer responsePropertiesAreRequired() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas == null || openApi.getPaths() == null) {
                return;
            }
            Deque<Schema<?>> pending = new ArrayDeque<>();
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(op -> {
                if (op.getResponses() == null) {
                    return;
                }
                op.getResponses().forEach((status, response) -> {
                    if (status.startsWith("2") && response.getContent() != null) {
                        response.getContent().values().forEach(media -> {
                            if (media.getSchema() != null) {
                                pending.add(media.getSchema());
                            }
                        });
                    }
                });
            }));

            Set<String> visited = new HashSet<>();
            while (!pending.isEmpty()) {
                Schema<?> schema = pending.pop();
                if (schema.get$ref() != null) {
                    String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
                    if (visited.add(name) && schemas.containsKey(name)) {
                        pending.add(schemas.get(name));
                    }
                    continue;
                }
                if (schema.getItems() != null) {
                    pending.add(schema.getItems());
                }
                if (schema.getProperties() != null) {
                    schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
                    schema.getProperties().values().forEach(pending::add);
                }
            }
        };
    }
    }
