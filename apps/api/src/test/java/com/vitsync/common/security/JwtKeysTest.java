package com.vitsync.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Validación de las claves RSA al arrancar. Las claves se generan en el test, nunca en el repo. */
class JwtKeysTest {

    @Test
    void pem_keys_are_loaded_even_with_literal_newlines() throws Exception {
        KeyPair pair = rsa(2048);
        // Así suele llegar un PEM en una variable de entorno: con "\n" escritos.
        String priv = pem("PRIVATE KEY", pair.getPrivate().getEncoded()).replace("\n", "\\n");
        String pub = pem("PUBLIC KEY", pair.getPublic().getEncoded());

        JwtKeys keys = JwtKeys.from(props(priv, pub, false));

        assertThat(keys.publicKey()).isEqualTo(pair.getPublic());
    }

    @Test
    void missing_keys_fail_unless_ephemeral_keys_are_allowed() {
        assertThatThrownBy(() -> JwtKeys.from(props(null, "", false)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("JWT_PRIVATE_KEY");
        assertThat(JwtKeys.from(props(null, null, true)).publicKey().getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void public_key_must_match_the_private_key() throws Exception {
        String priv = pem("PRIVATE KEY", rsa(2048).getPrivate().getEncoded());
        String otherPub = pem("PUBLIC KEY", rsa(2048).getPublic().getEncoded());

        assertThatThrownBy(() -> JwtKeys.from(props(priv, otherPub, false)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("does not match");
    }

    @Test
    void short_keys_and_garbage_are_rejected_without_echoing_them() throws Exception {
        KeyPair weak = rsa(1024);
        assertThatThrownBy(() -> JwtKeys.from(props(pem("PRIVATE KEY", weak.getPrivate().getEncoded()),
                pem("PUBLIC KEY", weak.getPublic().getEncoded()), false)))
                .hasMessageContaining("2048");

        assertThatThrownBy(() -> JwtKeys.from(props("not-a-key", "not-a-key-either", false)))
                .isInstanceOf(IllegalStateException.class)
                .hasNoCause()
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("not-a-key"));
    }

    private static JwtProperties props(String priv, String pub, boolean ephemeral) {
        return new JwtProperties(priv, pub, "vitsync-api", Duration.ofMinutes(15), ephemeral);
    }

    private static KeyPair rsa(int bits) throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(bits);
        return g.generateKeyPair();
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }
}
