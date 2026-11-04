package com.vitsync.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxBackoffTest {

    @Test
    void backoff_doubles_from_30_seconds_and_is_capped_at_one_hour() {
        assertThat(OutboxWorker.backoffFor(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(OutboxWorker.backoffFor(2)).isEqualTo(Duration.ofMinutes(1));
        assertThat(OutboxWorker.backoffFor(3)).isEqualTo(Duration.ofMinutes(2));
        assertThat(OutboxWorker.backoffFor(7)).isEqualTo(Duration.ofMinutes(32));
        // El 8.º sería 64 min: se queda en el tope.
        assertThat(OutboxWorker.backoffFor(8)).isEqualTo(Duration.ofHours(1));
        assertThat(OutboxWorker.backoffFor(100)).isEqualTo(Duration.ofHours(1));
    }
}
