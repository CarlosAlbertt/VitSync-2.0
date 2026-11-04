package com.vitsync.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del cifrado, sin Spring: un {@code new} y listo, se ejecutan en
 * milisegundos. Cada nombre describe la propiedad que protege (ADR-0008).
 */
class FieldEncryptorTest {

    // NIF sintético (el de ejemplo del Ministerio del Interior): nunca datos reales.
    private static final String NIF = "12345678Z";

    private final FieldEncryptor encryptor =
            new FieldEncryptor(new KeyRing(TestKeys.props(TestKeys.randomKey(), "v1", Map.of())));

    @Test
    void round_trip_returns_the_original_value() {
        assertThat(encryptor.decrypt(encryptor.encrypt(NIF))).isEqualTo(NIF);
        // También con texto no ASCII y con la cadena vacía, que son casos frontera reales
        // (alergias con tildes, un campo opcional enviado vacío).
        assertThat(encryptor.decrypt(encryptor.encrypt("Penicilina, ácaros"))).isEqualTo("Penicilina, ácaros");
        assertThat(encryptor.decrypt(encryptor.encrypt(""))).isEmpty();
    }

    @Test
    void stored_value_carries_the_current_key_version_and_no_plaintext() {
        String stored = encryptor.encrypt(NIF);

        assertThat(stored).startsWith("v1:").doesNotContain(NIF);
        // iv (12) + texto (9) + tag (16) bytes
        byte[] payload = Base64.getDecoder().decode(stored.substring(3));
        assertThat(payload).hasSize(FieldEncryptor.IV_BYTES + NIF.length() + FieldEncryptor.TAG_BITS / 8);
    }

    @Test
    void same_value_encrypts_differently_each_time() {
        // IV aleatorio: si dos cifrados coincidieran, se podría saber que dos pacientes
        // tienen el mismo dato sin descifrar nada (análisis de frecuencia).
        assertThat(encryptor.encrypt(NIF)).isNotEqualTo(encryptor.encrypt(NIF));
    }

    @Test
    void tampered_ciphertext_is_rejected() {
        String stored = encryptor.encrypt(NIF);
        byte[] payload = Base64.getDecoder().decode(stored.substring(3));
        payload[payload.length - 1] ^= 0x01;   // un solo bit cambiado en el tag
        String tampered = "v1:" + Base64.getEncoder().encodeToString(payload);

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("failed authentication");
    }

    @Test
    void error_messages_never_contain_the_value() {
        String stored = encryptor.encrypt(NIF);
        String truncated = stored.substring(0, stored.length() - 4) + "AAAA";

        assertThatThrownBy(() -> encryptor.decrypt(truncated))
                .isInstanceOf(EncryptionException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(NIF).doesNotContain(truncated));
    }

    @Test
    void value_from_a_previous_key_version_is_still_decryptable() {
        // Antes de rotar: se cifra con v1.
        String oldKey = TestKeys.randomKey();
        String storedWithV1 = new FieldEncryptor(new KeyRing(TestKeys.props(oldKey, "v1", Map.of())))
                .encrypt(NIF);

        // Después de rotar: v2 es la actual y v1 queda como anterior.
        FieldEncryptor rotated = new FieldEncryptor(
                new KeyRing(TestKeys.props(TestKeys.randomKey(), "v2", Map.of("v1", oldKey))));

        assertThat(rotated.decrypt(storedWithV1)).isEqualTo(NIF);
        assertThat(rotated.needsRotation(storedWithV1)).isTrue();
        assertThat(rotated.encrypt(NIF)).startsWith("v2:");
        assertThat(rotated.needsRotation(rotated.encrypt(NIF))).isFalse();
    }

    @Test
    void unknown_key_version_is_rejected() {
        String storedWithV9 = "v9:" + encryptor.encrypt(NIF).substring(3);

        assertThatThrownBy(() -> encryptor.decrypt(storedWithV9))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("Unknown key version");
    }

    @Test
    void version_prefix_is_authenticated() {
        // Misma clave registrada con dos versiones: si se cambia el prefijo a mano, el tag
        // no cuadra porque la versión entra en el AAD.
        String key = TestKeys.randomKey();
        FieldEncryptor twoVersions = new FieldEncryptor(
                new KeyRing(TestKeys.props(key, "v2", Map.of("v1", key))));
        String relabeled = "v1:" + twoVersions.encrypt(NIF).substring(3);

        assertThatThrownBy(() -> twoVersions.decrypt(relabeled))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("failed authentication");
    }

    @Test
    void malformed_values_are_rejected() {
        assertThatThrownBy(() -> encryptor.decrypt("no-prefix")).isInstanceOf(EncryptionException.class);
        assertThatThrownBy(() -> encryptor.decrypt("v1:***")).isInstanceOf(EncryptionException.class);
        assertThatThrownBy(() -> encryptor.decrypt("v1:AAAA")).isInstanceOf(EncryptionException.class);
        assertThatThrownBy(() -> encryptor.encrypt(null)).isInstanceOf(EncryptionException.class);
    }

    @Test
    void converter_maps_null_to_null_and_encrypts_the_rest() {
        EncryptedStringConverter converter = new EncryptedStringConverter(encryptor);

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        String column = converter.convertToDatabaseColumn(NIF);
        assertThat(column).startsWith("v1:");
        assertThat(converter.convertToEntityAttribute(column)).isEqualTo(NIF);
    }
}
