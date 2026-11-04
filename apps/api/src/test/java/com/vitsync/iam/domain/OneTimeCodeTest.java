package com.vitsync.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Reglas del código de un solo uso, sin BD ni Spring. */
class OneTimeCodeTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private OneTimeCode code() {
        return OneTimeCode.issue(1L, CodePurpose.EMAIL_VERIFICATION, "h".repeat(64), NOW);
    }

    @Test
    void correct_code_is_valid_only_once() {
        OneTimeCode c = code();
        assertThat(c.attempt(true, NOW.plusSeconds(60))).isEqualTo(OneTimeCode.Check.VALID);
        assertThat(c.attempt(true, NOW.plusSeconds(61))).isEqualTo(OneTimeCode.Check.INVALID);
    }

    @Test
    void code_expires_after_its_ttl() {
        OneTimeCode c = code();
        assertThat(c.getExpiresAt()).isEqualTo(NOW.plus(CodePurpose.EMAIL_VERIFICATION.ttl()));
        assertThat(c.attempt(true, c.getExpiresAt())).isEqualTo(OneTimeCode.Check.INVALID);
    }

    @Test
    void wrong_attempts_are_counted_and_the_last_one_exhausts_the_code() {
        OneTimeCode c = code();
        int max = CodePurpose.EMAIL_VERIFICATION.maxAttempts();
        for (int i = 1; i < max; i++) {
            assertThat(c.attempt(false, NOW)).isEqualTo(OneTimeCode.Check.INVALID);
        }
        assertThat(c.attempt(false, NOW)).isEqualTo(OneTimeCode.Check.EXHAUSTED);
        assertThat(c.getAttempts()).isEqualTo(max);
    }

    @Test
    void exhausted_code_is_rejected_even_when_correct() {
        OneTimeCode c = code();
        for (int i = 0; i < CodePurpose.EMAIL_VERIFICATION.maxAttempts(); i++) {
            c.attempt(false, NOW);
        }
        assertThat(c.attempt(true, NOW)).isEqualTo(OneTimeCode.Check.EXHAUSTED);
        assertThat(c.getConsumedAt()).isNull();
    }
}
