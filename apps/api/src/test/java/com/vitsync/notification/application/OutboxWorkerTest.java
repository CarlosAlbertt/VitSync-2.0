package com.vitsync.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.vitsync.notification.infrastructure.EmailSender;
import com.vitsync.support.IntegrationTest;
import com.vitsync.support.TestData;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * El outbox contra PostgreSQL real: encolado en transacción, envío, reintentos y FAILED.
 * El SMTP se simula ({@code @MockitoBean EmailSender}).
 */
class OutboxWorkerTest extends IntegrationTest {

    @Autowired EmailOutbox outbox;
    @Autowired OutboxWorker worker;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean EmailSender sender;

    @Test
    void enqueue_requires_the_callers_transaction() {
        // Sin transacción abierta, encolar es un error de programación: el email ya no iría
        // ligado al cambio que lo provoca.
        assertThatThrownBy(() -> outbox.enqueue(EmailTemplate.VERIFY_EMAIL, TestData.randomEmail(), Map.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void payload_is_stored_encrypted() {
        String to = TestData.randomEmail();
        long id = enqueue(to);

        String payload = jdbc.queryForObject("SELECT payload::text FROM outbox_messages WHERE id = ?", String.class, id);
        assertThat(payload).contains("\"data\"").contains("v1:").doesNotContain(to).doesNotContain("424242");
    }

    @Test
    void pending_message_is_sent_and_marked_sent() {
        String to = TestData.randomEmail();
        long id = enqueue(to);

        drain();

        verify(sender).send(eq(to), eq(EmailTemplate.VERIFY_EMAIL.subject()), anyString());
        assertThat(status(id)).isEqualTo("SENT");
    }

    @Test
    void failed_send_is_retried_later_and_marked_failed_after_the_last_attempt() {
        String to = TestData.randomEmail();
        willThrow(new MailSendException("smtp down for " + to)).given(sender).send(eq(to), anyString(), anyString());
        long id = enqueue(to);

        drain();

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, attempts, next_attempt_at, last_error FROM outbox_messages WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(((Number) row.get("attempts")).intValue()).isEqualTo(1);
        assertThat(((Timestamp) row.get("next_attempt_at")).toInstant()).isAfter(Instant.now());
        // El error se guarda sin el mensaje, que aquí contiene el destinatario.
        assertThat((String) row.get("last_error")).isEqualTo(MailSendException.class.getName()).doesNotContain(to);

        // Último intento: se fuerza a que toque ya, con 7 intentos previos. Un minuto en el
        // pasado y no now(): now() es la hora de PostgreSQL (en Docker) y el worker compara con
        // la de la JVM; si el reloj del contenedor va unos ms por delante, el mensaje aún no toca.
        jdbc.update("UPDATE outbox_messages SET attempts = 7, next_attempt_at = now() - interval '1 minute' "
                + "WHERE id = ?", id);
        drain();
        assertThat(status(id)).isEqualTo("FAILED");
    }

    private long enqueue(String to) {
        tx.executeWithoutResult(s -> outbox.enqueue(EmailTemplate.VERIFY_EMAIL, to,
                Map.of("firstName", "Lucía", "code", "424242", "minutes", "10")));
        return jdbc.queryForObject("SELECT max(id) FROM outbox_messages", Long.class);
    }

    // Procesa hasta vaciar lo que toca ahora (incluye mensajes de otros tests: la BD es común).
    private void drain() {
        while (worker.processBatch() > 0) {
            // sigue
        }
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT status FROM outbox_messages WHERE id = ?", String.class, id);
    }
}
