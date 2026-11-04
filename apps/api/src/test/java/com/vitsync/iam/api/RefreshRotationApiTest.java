package com.vitsync.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.support.IntegrationTest;
import com.vitsync.support.TestAccounts;
import com.vitsync.support.TestAccounts.Account;
import com.vitsync.support.TestAccounts.LoggedIn;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rotación del refresh token con detección de reuso (VIT-64). Los dos primeros tests son los
 * escenarios Gherkin de SCOPE.md, "Sesión y refresh".
 */
class RefreshRotationApiTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void renovacion_normal_returns_new_tokens_of_the_same_family_and_revokes_the_old_one() throws Exception {
        LoggedIn session = TestAccounts.login(mvc, TestAccounts.active(mvc, jdbc));
        String oldToken = session.refreshCookie().getValue();

        MockHttpServletResponse res = refresh(session.refreshCookie());

        assertThat(res.getStatus()).isEqualTo(200);
        Cookie newCookie = res.getCookie("vitsync_refresh");
        assertThat(newCookie.getValue()).isNotBlank().isNotEqualTo(oldToken);
        assertThat(TestAccounts.JSON.readTree(res.getContentAsString()).get("accessToken").asString())
                .isNotBlank().isNotEqualTo(session.accessToken());
        // Misma familia, y el anterior revocado.
        assertThat(familyOf(oldToken)).isEqualTo(familyOf(newCookie.getValue()));
        assertThat(isRevoked(oldToken)).isTrue();
        assertThat(isRevoked(newCookie.getValue())).isFalse();
    }

    @Test
    void reuso_de_un_refresh_revocado_revokes_the_whole_family_and_is_audited() throws Exception {
        LoggedIn session = TestAccounts.login(mvc, TestAccounts.active(mvc, jdbc));
        Cookie stolen = session.refreshCookie();
        Cookie legit = refresh(stolen).getCookie("vitsync_refresh");   // rotación normal: stolen -> legit

        MockHttpServletResponse reuse = refresh(stolen);                 // alguien reutiliza el viejo

        assertThat(reuse.getStatus()).isEqualTo(401);
        assertThat(TestAccounts.JSON.readTree(reuse.getContentAsString()).get("code").asString())
                .isEqualTo("INVALID_REFRESH_TOKEN");
        assertThat(reuse.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        // La familia entera cae: también el token "bueno" que se emitió después.
        assertThat(isRevoked(legit.getValue())).isTrue();
        assertThat(refresh(legit).getStatus()).isEqualTo(401);

        String family = familyOf(stolen.getValue());
        assertThat(jdbc.queryForMap("SELECT action, target_type, success, details::text AS details FROM audit_logs "
                + "WHERE target_ref = ?", family))
                .containsEntry("action", "REFRESH_TOKEN_REUSE")
                .containsEntry("target_type", "TOKEN_FAMILY")
                .containsEntry("success", false);
    }

    @Test
    void token_of_a_logged_out_session_is_rejected_without_a_reuse_alert() throws Exception {
        LoggedIn session = TestAccounts.login(mvc, TestAccounts.active(mvc, jdbc));
        mvc.perform(post("/api/v1/auth/logout").cookie(session.refreshCookie())).andExpect(status().isNoContent());

        assertThat(refresh(session.refreshCookie()).getStatus()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE target_ref = ?", Integer.class,
                familyOf(session.refreshCookie().getValue()))).isZero();
    }

    @Test
    void missing_unknown_or_expired_token_is_rejected() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        assertThat(refresh(new Cookie("vitsync_refresh", "not-a-real-token")).getStatus()).isEqualTo(401);

        LoggedIn session = TestAccounts.login(mvc, TestAccounts.active(mvc, jdbc));
        jdbc.update("UPDATE refresh_tokens SET expires_at = now() - interval '1 second' WHERE family_id = ?::uuid",
                familyOf(session.refreshCookie().getValue()));
        assertThat(refresh(session.refreshCookie()).getStatus()).isEqualTo(401);
    }

    @Test
    void suspended_account_cannot_refresh() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        LoggedIn session = TestAccounts.login(mvc, account);
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", account.email());

        assertThat(refresh(session.refreshCookie()).getStatus()).isEqualTo(401);
        assertThat(isRevoked(session.refreshCookie().getValue())).isTrue();
    }

    @Test
    void concurrent_refreshes_of_the_same_token_rotate_it_only_once() throws Exception {
        LoggedIn session = TestAccounts.login(mvc, TestAccounts.active(mvc, jdbc));
        int threads = 5;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Integer> call = () -> {
                    start.await();
                    return refresh(session.refreshCookie()).getStatus();
                };
                results.add(pool.submit(call));
            }
            start.countDown();   // todos a la vez
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : results) {
                statuses.add(f.get());
            }
            // El FOR UPDATE los pone en fila: uno rota y el resto ve el token ya canjeado.
            assertThat(statuses).containsOnlyOnce(200).filteredOn(s -> s != 200).containsOnly(401);
        } finally {
            pool.shutdownNow();
        }
    }

    private MockHttpServletResponse refresh(Cookie cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andReturn().getResponse();
    }

    private String familyOf(String token) {
        return jdbc.queryForObject("SELECT family_id::text FROM refresh_tokens WHERE token_hash = encode(sha256(?::bytea), 'hex')",
                String.class, token);
    }

    private boolean isRevoked(String token) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT revoked_at IS NOT NULL FROM refresh_tokens WHERE token_hash = encode(sha256(?::bytea), 'hex')",
                Boolean.class, token));
    }
}
