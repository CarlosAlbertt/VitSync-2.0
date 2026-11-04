package com.vitsync.iam.api;

import static com.vitsync.support.TestAccounts.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.support.IntegrationTest;
import com.vitsync.support.TestAccounts;
import com.vitsync.support.TestAccounts.Account;
import com.vitsync.support.TestAccounts.LoggedIn;
import com.vitsync.support.TestData;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Login, {@code /me} y logout de punta a punta (VIT-63), contra PostgreSQL real. */
class LoginApiTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void valid_credentials_return_an_access_token_and_set_the_refresh_cookie() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);

        String setCookie = login(account.email(), account.password())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(account.email()))
                .andExpect(jsonPath("$.user.role").value("PATIENT"))
                .andExpect(jsonPath("$.user.emailVerified").value(true))
                // El refresh no va nunca en el cuerpo.
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).startsWith("vitsync_refresh=").contains("HttpOnly", "Secure", "SameSite=Lax",
                "Path=/api/v1/auth", "Max-Age=604800");
        // En la BD solo está el hash del token de la cookie.
        String token = setCookie.substring("vitsync_refresh=".length(), setCookie.indexOf(';'));
        String hash = jdbc.queryForObject("SELECT t.token_hash FROM refresh_tokens t JOIN users u ON u.id = t.user_id "
                + "WHERE u.email = ?", String.class, account.email());
        assertThat(hash).hasSize(64).isNotEqualTo(token);
    }

    @Test
    void unknown_email_and_wrong_password_get_the_same_401() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);

        String wrongPassword = login(account.email(), "Not-The-Password-123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = login(TestData.randomEmail(), "Not-The-Password-123")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(withoutTraceId(wrongPassword)).isEqualTo(withoutTraceId(unknownEmail));
    }

    @Test
    void email_is_case_insensitive() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        login(account.email().toUpperCase(), account.password()).andExpect(status().isOk());
    }

    @Test
    void unverified_account_gets_403_and_a_new_code_only_when_the_previous_is_unusable() throws Exception {
        Account account = TestAccounts.registered(mvc);
        long firstCode = codeId(account.email());

        // Con un código todavía válido no se envía otro.
        login(account.email(), account.password())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_VERIFIED"));
        assertThat(codeId(account.email())).isEqualTo(firstCode);

        // Caducado: el login emite uno nuevo (y lo encola por email).
        jdbc.update("UPDATE one_time_codes SET expires_at = now() - interval '1 minute' WHERE id = ?", firstCode);
        login(account.email(), account.password()).andExpect(status().isForbidden());
        assertThat(codeId(account.email())).isNotEqualTo(firstCode);
    }

    @Test
    void unverified_account_with_wrong_password_gets_the_generic_401() throws Exception {
        Account account = TestAccounts.registered(mvc);
        login(account.email(), "Not-The-Password-123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void suspended_account_cannot_log_in() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", account.email());

        login(account.email(), account.password())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void me_requires_a_valid_bearer_token() throws Exception {
        LoggedIn session = TestAccounts.login(mvc, TestAccounts.active(mvc, jdbc));

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.body().get("user").get("id").asString()))
                .andExpect(jsonPath("$.firstName").value("Lucía"));

        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        // Un carácter cambiado en la firma: el mismo 401 con formato RFC 9457.
        String tampered = session.accessToken().substring(0, session.accessToken().length() - 2) + "xx";
        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void patch_me_updates_only_the_given_fields_and_encrypts_the_phone() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        LoggedIn session = TestAccounts.login(mvc, account);

        mvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("firstName", "Lucía María", "phone", "+34600111222"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Lucía María"))
                .andExpect(jsonPath("$.lastName").value("García"))
                .andExpect(jsonPath("$.phone").value("+34600111222"));

        String stored = jdbc.queryForObject("SELECT phone_encrypted FROM users WHERE email = ?", String.class, account.email());
        assertThat(stored).startsWith("v1:").doesNotContain("600111222");

        mvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @Test
    void logout_revokes_the_session_and_clears_the_cookie() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        LoggedIn session = TestAccounts.login(mvc, account);

        String setCookie = mvc.perform(post("/api/v1/auth/logout").cookie(session.refreshCookie()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).startsWith("vitsync_refresh=;").contains("Max-Age=0");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens t JOIN users u ON u.id = t.user_id "
                + "WHERE u.email = ? AND t.revoked_at IS NULL", Integer.class, account.email())).isZero();
        // Sin cookie también es 204: el resultado para el cliente es el mismo.
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private long codeId(String email) {
        return jdbc.queryForObject("SELECT c.id FROM one_time_codes c JOIN users u ON u.id = c.user_id "
                + "WHERE u.email = ? AND c.consumed_at IS NULL", Long.class, email);
    }

    private static String withoutTraceId(String problem) {
        return problem.replaceAll("\"traceId\":\"[^\"]*\"", "");
    }
}
