package com.vitsync.iam.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.notification.application.OutboxWorker;
import com.vitsync.notification.infrastructure.EmailSender;
import com.vitsync.support.IntegrationTest;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Recorrido completo de VIT-62: registro → el worker envía el email → se verifica con el
 * código que llega en él. El código se saca del email capturado, como lo haría una persona.
 */
class EmailVerificationApiTest extends IntegrationTest {

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired MockMvc mvc;
    @Autowired OutboxWorker worker;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean EmailSender sender;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void emailed_code_activates_the_account_once() throws Exception {
        String email = registerAndSend();
        String code = emailedCode(email);

        verifyCode(email, code).andExpect(status().isNoContent());
        assertThat(jdbc.queryForMap("SELECT status, email_verified_at FROM users WHERE email = ?", email))
                .containsEntry("status", "ACTIVE")
                .extractingByKey("email_verified_at").isNotNull();

        // Un solo uso.
        verifyCode(email, code).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_CODE"));
    }

    @Test
    void wrong_code_is_rejected_and_ten_failures_burn_the_code() throws Exception {
        String email = registerAndSend();
        String code = emailedCode(email);
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 1; i < 10; i++) {
            verifyCode(email, wrong).andExpect(status().isBadRequest());
        }
        verifyCode(email, wrong).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
        // Ni siquiera el código correcto sirve ya.
        verifyCode(email, code).andExpect(status().isTooManyRequests());
    }

    @Test
    void expired_code_is_rejected() throws Exception {
        String email = registerAndSend();
        String code = emailedCode(email);
        jdbc.update("UPDATE one_time_codes SET expires_at = now() - interval '1 minute' "
                + "WHERE user_id = (SELECT id FROM users WHERE email = ?)", email);

        verifyCode(email, code).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_CODE"));
    }

    @Test
    void unknown_email_gets_the_same_answer_as_a_wrong_code() throws Exception {
        verifyCode(TestData.randomEmail(), "123456").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_CODE"));
    }

    @Test
    void code_is_stored_hashed() throws Exception {
        String email = registerAndSend();
        String code = emailedCode(email);

        String hash = jdbc.queryForObject("SELECT code_hash FROM one_time_codes "
                + "WHERE user_id = (SELECT id FROM users WHERE email = ?)", String.class, email);
        assertThat(hash).hasSize(64).doesNotContain(code);
    }

    private String registerAndSend() throws Exception {
        String email = TestData.randomEmail();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email, "password", TestData.strongPassword(),
                                "firstName", "Lucía", "lastName", "García",
                                "nif", TestData.randomNif(), "birthDate", "1990-05-17"))))
                .andExpect(status().isCreated());
        while (worker.processBatch() > 0) {
            // vacía el outbox (también los mensajes de otros tests: la BD es común)
        }
        return email;
    }

    private String emailedCode(String email) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).send(eq(email), anyString(), body.capture());
        Matcher m = CODE.matcher(body.getValue());
        assertThat(m.find()).as("email body contains a 6-digit code").isTrue();
        return m.group(1);
    }

    private ResultActions verifyCode(String email, String code) throws Exception {
        return mvc.perform(post("/api/v1/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "code", code))));
    }
}
