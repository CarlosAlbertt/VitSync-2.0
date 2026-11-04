package com.vitsync.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Validaciones de arranque: cada caso es una configuración con la que la aplicación NO debe
 * levantar. Todos comprueban además que el mensaje nombra la propiedad sin citar la clave.
 */
class KeyRingTest {

    @Test
    void valid_configuration_exposes_current_and_previous_versions() {
        String current = TestKeys.randomKey();
        String previous = TestKeys.randomKey();
        KeyRing ring = new KeyRing(TestKeys.props(current, "v2", Map.of("v1", previous)));

        assertThat(ring.currentVersion()).isEqualTo("v2");
        assertThat(ring.keyFor("v1")).isPresent();
        assertThat(ring.keyFor("v3")).isEmpty();
        assertThat(ring.toString()).doesNotContain(current).doesNotContain(previous);
    }

    @Test
    void missing_key_is_rejected() {
        assertThatThrownBy(() -> new KeyRing(TestKeys.props(null, "v1", Map.of())))
                .isInstanceOf(EncryptionException.class).hasMessageContaining("ENCRYPTION_KEY");
        assertThatThrownBy(() -> new KeyRing(TestKeys.props("  ", "v1", Map.of())))
                .isInstanceOf(EncryptionException.class).hasMessageContaining("is required");
    }

    @Test
    void key_that_is_not_base64_is_rejected_without_echoing_it() {
        String notBase64 = "this is not base64!!";
        assertThatThrownBy(() -> new KeyRing(TestKeys.props(notBase64, "v1", Map.of())))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("not valid base64")
                .hasNoCause()
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(notBase64));
    }

    @Test
    void key_with_wrong_length_is_rejected() {
        String sixteenBytes = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new KeyRing(TestKeys.props(sixteenBytes, "v1", Map.of())))
                .isInstanceOf(EncryptionException.class).hasMessageContaining("32 bytes");
    }

    @Test
    void version_must_match_v_number() {
        for (String bad : new String[] {null, "", "1", "V1", "v", "v1a", "version1"}) {
            assertThatThrownBy(() -> new KeyRing(TestKeys.props(TestKeys.randomKey(), bad, Map.of())))
                    .as("version %s", bad)
                    .isInstanceOf(EncryptionException.class).hasMessageContaining("v<number>");
        }
    }

    @Test
    void previous_keys_are_validated_too() {
        assertThatThrownBy(() -> new KeyRing(TestKeys.props(TestKeys.randomKey(), "v2", Map.of("old", TestKeys.randomKey()))))
                .isInstanceOf(EncryptionException.class).hasMessageContaining("previous-keys.old");
        assertThatThrownBy(() -> new KeyRing(TestKeys.props(TestKeys.randomKey(), "v2", Map.of("v1", "short"))))
                .isInstanceOf(EncryptionException.class).hasMessageContaining("previous-keys.v1");
        assertThatThrownBy(() -> new KeyRing(TestKeys.props(TestKeys.randomKey(), "v2", Map.of("v2", TestKeys.randomKey()))))
                .isInstanceOf(EncryptionException.class).hasMessageContaining("duplicates");
    }

    @Test
    void insecure_dev_key_is_rejected_unless_explicitly_allowed() {
        assertThatThrownBy(() -> new KeyRing(TestKeys.props(TestKeys.ZERO_KEY, "v1", Map.of())))
                .isInstanceOf(EncryptionException.class).hasMessageContaining("insecure development key");

        CryptoProperties allowed = new CryptoProperties(TestKeys.ZERO_KEY, "v1", TestKeys.PEPPER, Map.of(), true);
        assertThat(new KeyRing(allowed).currentVersion()).isEqualTo("v1");
    }

    @Test
    void properties_to_string_does_not_leak_secrets() {
        String key = TestKeys.randomKey();
        CryptoProperties props = TestKeys.props(key, "v1", Map.of());
        assertThat(props.toString()).doesNotContain(key).doesNotContain(TestKeys.PEPPER);
    }
}
