package com.vitsync.common.crypto;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Claves para los tests unitarios. Se generan al vuelo en vez de escribirlas en el código:
 * así no hay ningún valor con pinta de secreto en el repositorio que haga saltar a gitleaks,
 * y cada ejecución usa claves distintas (un test que solo pasa con una clave concreta
 * estaría probando la clave, no el código).
 */
final class TestKeys {

    static final String ZERO_KEY = Base64.getEncoder().encodeToString(new byte[KeyRing.KEY_BYTES]);
    static final String PEPPER = "unit-test-pepper-00000000000000000";

    private static final SecureRandom RANDOM = new SecureRandom();

    private TestKeys() {
    }

    static String randomKey() {
        byte[] raw = new byte[KeyRing.KEY_BYTES];
        RANDOM.nextBytes(raw);
        return Base64.getEncoder().encodeToString(raw);
    }

    static CryptoProperties props(String key, String version, Map<String, String> previous) {
        return new CryptoProperties(key, version, PEPPER, previous, false);
    }
}
