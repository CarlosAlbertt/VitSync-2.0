package com.vitsync.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.common.crypto.BlindIndex;
import com.vitsync.iam.domain.UserStatus;
import com.vitsync.iam.infrastructure.BreachedPasswordChecker;
import com.vitsync.iam.infrastructure.UserRepository;
import com.vitsync.support.IntegrationTest;
import com.vitsync.support.TestData;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code POST /api/v1/auth/register} de punta a punta, contra PostgreSQL real.
 *
 * <p>HaveIBeenPwned se simula con {@code @MockitoBean}: sustituye el bean real en el contexto
 * de este test, y cada test decide qué responde.
 */
class RegistrationApiTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired BlindIndex blindIndex;
    @Autowired TransactionTemplate tx;

    @MockitoBean BreachedPasswordChecker breachedPasswords;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void valid_registration_creates_a_pending_patient_with_its_profile() throws Exception {
        String email = TestData.randomEmail();

        register(email, TestData.randomNif())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                // Nunca el id interno (decisión D6).
                .andExpect(jsonPath("$.id").doesNotExist());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT u.role, u.status, u.password_hash, p.user_id AS profile FROM users u "
                        + "LEFT JOIN patient_profiles p ON p.user_id = u.id WHERE u.email = ?", email);
        assertThat(row.get("role")).isEqualTo("PATIENT");
        assertThat(row.get("status")).isEqualTo(UserStatus.PENDING_VERIFICATION.name());
        // ADR-0003: no puede quedar un paciente sin perfil.
        assertThat(row.get("profile")).isNotNull();
        assertThat((String) row.get("password_hash")).startsWith("{bcrypt}$2a$");
    }

    @Test
    void nif_is_unreadable_in_the_database_and_readable_through_the_entity() throws Exception {
        String email = TestData.randomEmail();
        String nif = TestData.randomNif();
        register(email, nif.toLowerCase()).andExpect(status().isCreated());

        // Lo que vería alguien con un volcado de la BD: cifrado versionado y una huella.
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT nif_encrypted, nif_hash FROM users WHERE email = ?", email);
        assertThat((String) row.get("nif_encrypted")).startsWith("v1:").doesNotContain(nif);
        assertThat(((String) row.get("nif_hash")).trim()).isEqualTo(blindIndex.hash(nif));

        // Lo que ve la aplicación: el valor en claro, ya normalizado.
        String decrypted = tx.execute(s -> users.findByEmail(email).orElseThrow().getNif());
        assertThat(decrypted).isEqualTo(nif);
    }

    @Test
    void duplicate_email_and_duplicate_nif_get_the_same_409() throws Exception {
        String email = TestData.randomEmail();
        String nif = TestData.randomNif();
        register(email, nif).andExpect(status().isCreated());

        String byEmail = register(email.toUpperCase(), TestData.randomNif())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_EXISTS"))
                .andReturn().getResponse().getContentAsString();
        // Mismo NIF con otro formato: el blind index normaliza y lo detecta igual.
        String byNif = register(TestData.randomEmail(), " " + nif.toLowerCase())
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        // Mismo cuerpo salvo el traceId: la respuesta no dice qué campo choca.
        assertThat(withoutTraceId(byEmail)).isEqualTo(withoutTraceId(byNif));
        assertThat(byEmail).doesNotContainIgnoringCase("email").doesNotContainIgnoringCase("nif");
    }

    @Test
    void invalid_nif_is_rejected_with_the_failing_field() throws Exception {
        register(TestData.randomEmail(), "12345678A")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("nif"));
    }

    @Test
    void short_password_is_rejected() throws Exception {
        post(body(TestData.randomEmail(), "short-pass", TestData.randomNif()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void breached_password_is_rejected_and_nothing_is_stored() throws Exception {
        given(breachedPasswords.isBreached(anyString())).willReturn(true);
        String email = TestData.randomEmail();

        register(email, TestData.randomNif())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_BREACHED"));
        assertThat(users.findByEmail(email)).isEmpty();
    }

    private ResultActions register(String email, String nif) throws Exception {
        return post(body(email, TestData.strongPassword(), nif));
    }

    private ResultActions post(String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String body(String email, String password, String nif) {
        return json.writeValueAsString(Map.of(
                "email", email, "password", password, "firstName", "Lucía", "lastName", "García",
                "nif", nif, "birthDate", "1990-05-17"));
    }

    private String withoutTraceId(String problem) {
        return problem.replaceAll("\"traceId\":\"[^\"]*\"", "");
    }
}
