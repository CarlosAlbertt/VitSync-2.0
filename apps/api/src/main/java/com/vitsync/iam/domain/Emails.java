package com.vitsync.iam.domain;

import java.util.Locale;

/**
 * Normalización de emails.
 *
 * <p>La columna es {@code citext}, pero no basta con fiarse de ella: el driver JDBC envía los
 * parámetros como {@code varchar}, y {@code citext = varchar} acaba comparándose como texto
 * normal, que distingue mayúsculas. Normalizando siempre en la aplicación (al guardar y al
 * buscar), las dos cosas coinciden y no hay sorpresas.
 */
public final class Emails {

    private Emails() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
