package com.vitsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guardián del contrato: falla si docs/openapi.json no coincide con lo que genera el código.
 *
 * <p>Así, cualquier cambio en la API (un campo renombrado, un endpoint nuevo) obliga a
 * actualizar el fichero en el mismo PR, y el cambio de contrato queda visible en la revisión.
 * Como corre dentro de {@code ./mvnw verify}, la CI lo comprueba sin configuración extra.
 *
 * <p>Para regenerar el fichero: {@code ./mvnw verify -Dopenapi.update=true}.
 */
// Arranca la aplicación completa, con la documentación encendida solo para este test.
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc   // permite hacer peticiones HTTP simuladas sin levantar un servidor
@Testcontainers
class OpenApiContractTest {

    // Maven ejecuta los tests desde apps/api.
    private static final Path CONTRACT = Path.of("..", "..", "docs", "openapi.json");

    // La app completa necesita base de datos: PostgreSQL real en Docker (ADR-0008).
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void committed_contract_matches_generated_spec() throws Exception {
        // 1. Pide la especificación que genera springdoc en este momento.
        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode generated = mapper.readTree(body);

        // 2. Modo actualización: escribe la spec en el fichero y termina.
        if (Boolean.getBoolean("openapi.update")) {
            Files.writeString(CONTRACT,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generated) + "\n");
            return;
        }

        // 3. Modo normal: compara como árboles JSON (no como texto), así los saltos de
        //    línea de Windows o el sangrado no provocan falsos fallos.
        assertThat(mapper.readTree(Files.readString(CONTRACT)))
                .as("docs/openapi.json is stale: run ./mvnw verify -Dopenapi.update=true and commit it")
                .isEqualTo(generated);
    }
}
