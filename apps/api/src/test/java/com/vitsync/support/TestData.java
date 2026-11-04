package com.vitsync.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Datos sintéticos para los tests. Nunca datos reales (CONTRIBUTING). */
public final class TestData {

    private static final String LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    private TestData() {
    }

    /** DNI aleatorio con su letra de control correcta. */
    public static String randomNif() {
        int number = ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);
        return number + String.valueOf(LETTERS.charAt(number % 23));
    }

    public static String randomEmail() {
        return "user-" + UUID.randomUUID() + "@vitsync.test";
    }

    /** Pasa la política (12+ caracteres) y no está en ninguna lista de filtradas. */
    public static String strongPassword() {
        return "Correct-Horse-" + UUID.randomUUID();
    }
}
