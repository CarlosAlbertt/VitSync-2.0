package com.vitsync.iam.api;

import static com.vitsync.support.TestAccounts.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.iam.infrastructure.BreachedPasswordChecker;
import com.vitsync.notification.application.EmailTemplate;
import com.vitsync.notification.application.OutboxWorker;
import com.vitsync.notification.infrastructure.EmailSender;
import com.vitsync.support.IntegrationTest;
import com.vitsync.support.TestAccounts;
import com.vitsync.support.TestAccounts.Account;
import com.vitsync.support.TestData;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Recuperación de contraseña por código de un solo uso (VIT-65), de punta a punta. */
class PasswordResetApiTest extends IntegrationTest {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired OutboxWorker worker;

    @MockitoBean EmailSender sender;
    @MockitoBean BreachedPasswordChecker breachedPasswords;

    @Test
    void request_answers_the_same_whether_or_not_the_account_exists() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);

        String existing = request(account.email()).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String unknown = request(TestData.randomEmail()).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(existing).isEqualTo(unknown).isEmpty();
        assertThat(emailedCode(account.email())).hasSize(6);
    }

    @Test
    void valid_code_changes_the_password_and_closes_every_session() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        // Dos sesiones abiertas (dos dispositivos): las dos tienen que caer.
        TestAccounts.login(mvc, account);
        TestAccounts.login(mvc, account);
        request(account.email());
        String newPassword = TestData.strongPassword();

        reset(account.email(), emailedCode(account.email()), newPassword).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens t JOIN users u ON u.id = t.user_id "
                + "WHERE u.email = ? AND t.revoked_at IS NULL", Integer.class, account.email())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs a JOIN users u ON a.actor_ref = u.public_id::text "
                + "WHERE u.email = ? AND a.action = 'PASSWORD_RESET'", Integer.class, account.email())).isOne();
        login(account.email(), account.password()).andExpect(status().isUnauthorized());
        login(account.email(), newPassword).andExpect(status().isOk());
    }

    @Test
    void code_works_only_once() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        request(account.email());
        String code = emailedCode(account.email());

        reset(account.email(), code, TestData.strongPassword()).andExpect(status().isNoContent());
        reset(account.email(), code, TestData.strongPassword()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_CODE"));
    }

    @Test
    void five_wrong_codes_burn_the_code() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        request(account.email());
        String code = emailedCode(account.email());
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 1; i < 5; i++) {
            reset(account.email(), wrong, TestData.strongPassword()).andExpect(status().isBadRequest());
        }
        reset(account.email(), wrong, TestData.strongPassword()).andExpect(status().isTooManyRequests());
        reset(account.email(), code, TestData.strongPassword()).andExpect(status().isTooManyRequests());
        login(account.email(), account.password()).andExpect(status().isOk());
    }

    @Test
    void breached_new_password_is_rejected_without_spending_the_code() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        request(account.email());
        String code = emailedCode(account.email());
        given(breachedPasswords.isBreached(anyString())).willReturn(true);

        reset(account.email(), code, TestData.strongPassword()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_BREACHED"));

        given(breachedPasswords.isBreached(anyString())).willReturn(false);
        reset(account.email(), code, TestData.strongPassword()).andExpect(status().isNoContent());
    }

    @Test
    void repeated_requests_within_a_minute_send_a_single_email() throws Exception {
        Account account = TestAccounts.active(mvc, jdbc);
        request(account.email());
        request(account.email());
        drain();

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).send(to.capture(), eq(EmailTemplate.PASSWORD_RESET.subject()), anyString());
        assertThat(to.getAllValues()).containsOnlyOnce(account.email());
    }

    @Test
    void unverified_account_gets_no_reset_code() throws Exception {
        Account account = TestAccounts.registered(mvc);
        request(account.email()).andExpect(status().isAccepted());
        drain();

        verify(sender, never()).send(eq(account.email()), eq(EmailTemplate.PASSWORD_RESET.subject()), anyString());
    }

    private String emailedCode(String email) {
        drain();
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).send(eq(email), eq(EmailTemplate.PASSWORD_RESET.subject()), body.capture());
        Matcher m = CODE.matcher(body.getValue());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private void drain() {
        while (worker.processBatch() > 0) {
            // vacía el outbox
        }
    }

    private ResultActions request(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email))));
    }

    private ResultActions reset(String email, String code, String newPassword) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "code", code, "newPassword", newPassword))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "password", password))));
    }
}
