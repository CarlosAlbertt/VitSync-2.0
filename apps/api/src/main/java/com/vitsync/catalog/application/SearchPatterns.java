package com.vitsync.catalog.application;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Convierte el texto que escribe el usuario en patrones {@code LIKE} seguros.
 *
 * <p>Escapa {@code %}, {@code _} y {@code \}: sin eso, buscar "50%" o "_" se interpretaría
 * como comodín. No es un problema de inyección (va como parámetro), sino de resultados.
 */
final class SearchPatterns {

    /** Coincide con todo: lo que se usa cuando no hay texto de búsqueda. */
    static final String ANY = "%";

    private SearchPatterns() {
    }

    /** {@code "Cardio"} → {@code "%cardio%"}, para comparar con {@code lower(name)}. */
    static String contains(String text) {
        if (text == null || text.isBlank()) {
            return ANY;
        }
        return "%" + escape(text.strip().toLowerCase(Locale.ROOT)) + "%";
    }

    /**
     * El mismo texto con la forma de un slug: sin tildes y con guiones. Así "cardiologia" o
     * "medicina familiar" encuentran "Cardiología" y "Medicina familiar" por su slug.
     */
    static String containsAsSlug(String text) {
        if (text == null || text.isBlank()) {
            return ANY;
        }
        String plain = Normalizer.normalize(text.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", "-");
        return "%" + escape(plain) + "%";
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
