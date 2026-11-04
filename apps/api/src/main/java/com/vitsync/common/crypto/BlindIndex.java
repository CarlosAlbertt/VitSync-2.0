package com.vitsync.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Blind index: huella HMAC-SHA256 de un valor para poder buscarlo sin descifrar (ADR-0005).
 *
 * <p>El cifrado con IV aleatorio hace que {@code WHERE nif_encrypted = ?} no funcione nunca.
 * La solución es guardar al lado {@code nif_hash = HMAC(pepper, valor normalizado)}: el mismo
 * NIF da siempre la misma huella, y la columna admite un índice único normal.
 *
 * <p>Por qué HMAC con pepper y no un SHA-256 a secas: los NIF son un espacio pequeño (8
 * dígitos y una letra). Con SHA-256 sin secreto, quien robe la BD calcula los cien millones de
 * hashes posibles en minutos y revierte la columna. Con el pepper fuera de la BD, no puede.
 *
 * <p>Normalización: {@code trim} y mayúsculas, para que {@code " 12345678z"} y
 * {@code "12345678Z"} se consideren el mismo NIF y el índice único los detecte como duplicado.
 */
public final class BlindIndex {

    /** 32 caracteres: por debajo, un pepper corto se podría adivinar por fuerza bruta. */
    static final int MIN_PEPPER_LENGTH = 32;
    private static final String HMAC = "HmacSHA256";

    private final SecretKeySpec pepper;

    public BlindIndex(String pepper) {
        if (pepper == null || pepper.strip().length() < MIN_PEPPER_LENGTH) {
            // Como con las claves: se nombra la propiedad, nunca se repite el valor.
            throw new EncryptionException("vitsync.crypto.blind-index-pepper (BLIND_INDEX_PEPPER) must be at least "
                    + MIN_PEPPER_LENGTH + " characters");
        }
        this.pepper = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC);
    }

    /** Huella en hexadecimal (64 caracteres), lista para la columna {@code char(64)}. */
    public String hash(String value) {
        if (value == null) {
            throw new EncryptionException("Cannot hash null");
        }
        try {
            // Mac no es thread-safe: uno nuevo por llamada. Crearlo cuesta microsegundos.
            Mac mac = Mac.getInstance(HMAC);
            mac.init(pepper);
            byte[] digest = mac.doFinal(normalize(value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Blind index computation failed", e);
        }
    }

    static String normalize(String value) {
        // Locale.ROOT: con la configuración regional turca, "i".toUpperCase() no da "I".
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
