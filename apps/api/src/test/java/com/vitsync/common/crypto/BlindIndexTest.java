package com.vitsync.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BlindIndexTest {

    private final BlindIndex index = new BlindIndex(TestKeys.PEPPER);

    @Test
    void hash_is_64_hex_characters_and_deterministic() {
        String hash = index.hash("12345678Z");

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(index.hash("12345678Z")).isEqualTo(hash);
    }

    @Test
    void hash_normalizes_whitespace_and_case() {
        // Sin normalizar, " 12345678z" pasaría el índice único como si fuera otro NIF.
        assertThat(index.hash(" 12345678z\t")).isEqualTo(index.hash("12345678Z"));
    }

    @Test
    void different_values_produce_different_hashes() {
        assertThat(index.hash("12345678Z")).isNotEqualTo(index.hash("87654321X"));
    }

    @Test
    void hash_depends_on_the_pepper() {
        // Sin el pepper no se puede recalcular la huella: es lo que impide revertir la
        // columna por fuerza bruta con una BD robada.
        BlindIndex other = new BlindIndex("another-unit-test-pepper-11111111111");
        assertThat(other.hash("12345678Z")).isNotEqualTo(index.hash("12345678Z"));
    }

    @Test
    void short_or_missing_pepper_is_rejected_without_echoing_it() {
        assertThatThrownBy(() -> new BlindIndex("too-short-pepper"))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("at least 32")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("too-short-pepper"));
        assertThatThrownBy(() -> new BlindIndex(null)).isInstanceOf(EncryptionException.class);
    }
}
