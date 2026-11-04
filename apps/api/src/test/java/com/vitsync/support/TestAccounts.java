package com.vitsync.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Atajos para los tests que necesitan una cuenta: se registra por la API (el camino real) y,
 * cuando hace falta activa, se marca verificada directamente en la BD. La verificación por
 * código ya la cubre su propio test; repetirla aquí solo haría los tests más lentos y frágiles.
 */
public final class TestAccounts {

    public static final JsonMapper JSON = JsonMapper.builder().build();

    private TestAccounts() {
    }

    public record Account(String email, String password) {
    }

    public record LoggedIn(String accessToken, Cookie refreshCookie, JsonNode body) {
    }

    public static Account registered(MockMvc mvc) throws Exception {
        Account account = new Account(TestData.randomEmail(), TestData.strongPassword());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of(
                                "email", account.email(), "password", account.password(),
                                "firstName", "Lucía", "lastName", "García",
                                "nif", TestData.randomNif(), "birthDate", "1990-05-17"))))
                .andExpect(status().isCreated());
        return account;
    }

    public static Account active(MockMvc mvc, JdbcTemplate jdbc) throws Exception {
        Account account = registered(mvc);
        jdbc.update("UPDATE users SET status = 'ACTIVE', email_verified_at = now() WHERE email = ?", account.email());
        return account;
    }

    public static LoggedIn login(MockMvc mvc, Account account) throws Exception {
        MockHttpServletResponse res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("email", account.email(), "password", account.password()))))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        JsonNode body = JSON.readTree(res.getContentAsString());
        return new LoggedIn(body.get("accessToken").asString(), res.getCookie("vitsync_refresh"), body);
    }
}
