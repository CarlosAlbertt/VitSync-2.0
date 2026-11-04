package com.vitsync.iam.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validación de NIF (DNI) y NIE españoles con su letra de control.
 *
 * <p>Algoritmo oficial (Ministerio del Interior): la letra es
 * {@code "TRWAGMYFPDXBNJZSQVHLCKE".charAt(número % 23)}. En el NIE, la letra inicial se
 * sustituye por un dígito (X→0, Y→1, Z→2) antes de calcular.
 *
 * <p>Es dominio puro, sin Spring ni anotaciones: lo usan la validación del DTO y el servicio,
 * y se prueba con un test unitario de milisegundos.
 */
public final class Nif {

    private static final String LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final Pattern DNI = Pattern.compile("^\\d{8}[A-Z]$");
    private static final Pattern NIE = Pattern.compile("^[XYZ]\\d{7}[A-Z]$");

    private Nif() {
    }

    /**
     * Forma canónica: sin espacios ni guiones y en mayúsculas ({@code "12345678-z"} →
     * {@code "12345678Z"}). Es la que se cifra y la que entra en el blind index.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** {@code true} si es un DNI o un NIE con la letra de control correcta. */
    public static boolean isValid(String raw) {
        String nif = normalize(raw);
        if (nif == null) {
            return false;
        }
        String digits;
        if (DNI.matcher(nif).matches()) {
            digits = nif.substring(0, 8);
        } else if (NIE.matcher(nif).matches()) {
            // X, Y, Z → 0, 1, 2: su posición en "XYZ".
            digits = "XYZ".indexOf(nif.charAt(0)) + nif.substring(1, 8);
        } else {
            return false;
        }
        char expected = LETTERS.charAt(Integer.parseInt(digits) % 23);
        return nif.charAt(nif.length() - 1) == expected;
    }
}
