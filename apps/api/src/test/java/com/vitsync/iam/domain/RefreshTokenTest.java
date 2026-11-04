package com.vitsync.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private RefreshToken token() {
        User user = User.newPatient("a@vitsync.test", "{bcrypt}x", "A", "B", "12345678Z", "h", null,
                LocalDate.of(1990, 1, 1));
        return RefreshToken.issue(user, "h".repeat(64), UUID.randomUUID(), "ua".repeat(400), NOW);
    }

    @Test
    void lives_seven_days() {
        RefreshToken t = token();
        assertThat(t.isExpired(NOW.plus(RefreshToken.TTL).minusSeconds(1))).isFalse();
        assertThat(t.isExpired(NOW.plus(RefreshToken.TTL))).isTrue();
    }

    @Test
    void only_a_consumed_token_counts_as_reuse() {
        RefreshToken rotated = token();
        rotated.consume(NOW);
        assertThat(rotated.wasConsumed()).isTrue();

        RefreshToken loggedOut = token();
        loggedOut.revoke(NOW);
        assertThat(loggedOut.isRevoked()).isTrue();
        assertThat(loggedOut.wasConsumed()).isFalse();
    }

    @Test
    void user_agent_is_truncated_to_the_column_size() {
        assertThat(token().getUserAgent()).hasSize(512);
    }
}
