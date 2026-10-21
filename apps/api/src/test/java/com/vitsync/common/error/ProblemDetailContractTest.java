package com.vitsync.common.error;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.common.config.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprueba que todos los errores salen con el mismo formato RFC 9457 (code + traceId).
 *
 * <p>{@code @WebMvcTest} levanta solo la capa web (controladores, filtros, advice), sin base
 * de datos: es rápido y no necesita Docker. SecurityConfig se importa a mano porque esta
 * "rebanada" de la aplicación no carga las clases de configuración por su cuenta.
 *
 * <p>El nombre de cada test describe la regla que protege, como pide el ADR-0008.
 */
@WebMvcTest(controllers = ProblemDetailContractTest.ProbeController.class)
@Import({SecurityConfig.class, ProblemDetailContractTest.ProbeController.class})
class ProblemDetailContractTest {

    // Cliente HTTP simulado: lanza peticiones contra la app sin servidor real.
    @Autowired
    MockMvc mvc;

    // Sin .with(user(...)) la petición va sin usuario: la seguridad debe cortarla con 401.
    @Test
    void anonymous_request_gets_401_problem_with_trace_id() throws Exception {
        mvc.perform(get("/probe/business"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    // .with(user("u")) simula un usuario autenticado sin necesidad de token.
    @Test
    void business_exception_maps_to_its_error_code() throws Exception {
        mvc.perform(get("/probe/business").with(user("u")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.type").value("https://api.vitsync.es/errors/resource-not-found"));
    }

    // Cuerpo con name vacío: @NotBlank falla y la respuesta debe decir qué campo es.
    @Test
    void invalid_body_lists_the_failing_fields() throws Exception {
        mvc.perform(post("/probe/validated").with(user("u"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    // El controlador lanza "secret internal detail": ese texto NO puede aparecer en la respuesta.
    @Test
    void unexpected_exception_does_not_leak_its_message() throws Exception {
        mvc.perform(get("/probe/boom").with(user("u")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    // Usuario PATIENT contra un método que exige ADMIN: autenticado, pero sin permiso -> 403.
    @Test
    void wrong_role_gets_403_problem() throws Exception {
        mvc.perform(get("/probe/admin").with(user("u").roles("PATIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // Si el cliente manda un X-Request-Id válido, se respeta en la cabecera y en el error.
    @Test
    void valid_incoming_request_id_is_echoed() throws Exception {
        mvc.perform(get("/probe/business").with(user("u")).header("X-Request-Id", "abc12345-test"))
                .andExpect(header().string("X-Request-Id", "abc12345-test"))
                .andExpect(jsonPath("$.traceId").value("abc12345-test"));
    }

    // Ruta inexistente: el 404 lo genera Spring, pero debe salir con nuestro formato.
    @Test
    void framework_errors_also_carry_code() throws Exception {
        mvc.perform(get("/probe/does-not-exist").with(user("u")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    /**
     * Controlador de prueba: cada endpoint provoca un tipo de error concreto.
     *
     * <p>Va anidado dentro del test a propósito: Spring Boot excluye del escaneo las clases
     * anidadas en tests, así que no se cuela en la aplicación real ni en openapi.json.
     */
    @RestController
    static class ProbeController {

        record Body(@NotBlank String name) {}

        @GetMapping("/probe/business")
        void business() { throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); }

        @PostMapping("/probe/validated")
        void validated(@Valid @RequestBody Body body) {}

        @GetMapping("/probe/boom")
        void boom() { throw new IllegalStateException("secret internal detail"); }

        @GetMapping("/probe/admin")
        @PreAuthorize("hasRole('ADMIN')")
        void admin() {}
    }
}
