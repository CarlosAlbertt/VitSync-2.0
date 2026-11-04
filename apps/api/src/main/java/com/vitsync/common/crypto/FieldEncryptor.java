package com.vitsync.common.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Cifra y descifra campos de texto con AES-256-GCM (ADR-0005).
 *
 * <p>Formato guardado: {@code <versión>:base64(iv || cifrado+tag)}, p. ej. {@code v1:3q2+7w...}.
 * <ul>
 *   <li><b>GCM</b> es cifrado autenticado: además de ocultar, el tag de 128 bits detecta
 *       cualquier cambio en el cifrado. Un byte alterado en la BD no da basura: da error.
 *   <li><b>IV aleatorio de 12 bytes</b> por cada cifrado (el tamaño que recomienda NIST SP
 *       800-38D para GCM). Por eso el mismo NIF cifrado dos veces da dos textos distintos, y
 *       por eso para buscar hace falta el {@link BlindIndex}. Reutilizar un IV con la misma
 *       clave rompería GCM por completo: de ahí {@link SecureRandom} y no un contador.
 *   <li><b>La versión</b> elige la clave al descifrar, lo que permite rotarla. Además se pasa
 *       como AAD (datos autenticados no cifrados): si alguien cambia {@code v1:} por
 *       {@code v2:} en la BD, el tag tampoco cuadra.
 * </ul>
 *
 * <p>Es thread-safe: no guarda estado mutable; cada llamada crea su propio {@link Cipher}
 * (que no lo es). {@link SecureRandom} sí admite uso concurrente.
 *
 * <p>Ningún mensaje de error ni log contiene el valor en claro ni el cifrado.
 */
public final class FieldEncryptor {

    static final int IV_BYTES = 12;
    static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final char SEPARATOR = ':';

    private final KeyRing keyRing;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryptor(KeyRing keyRing) {
        this.keyRing = keyRing;
    }

    /** Cifra con la clave actual. Nunca devuelve el mismo texto para el mismo valor. */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new EncryptionException("Cannot encrypt null; map null before calling");
        }
        String version = keyRing.currentVersion();
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, keyRing.currentKey(), iv, version);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            // iv || cifrado: el IV no es secreto, pero hace falta para descifrar, así que viaja
            // delante. El tag de GCM ya va pegado al final del cifrado.
            byte[] out = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv).put(ciphertext).array();
            return version + SEPARATOR + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    /** Descifra un valor de cualquier versión conocida por el {@link KeyRing}. */
    public String decrypt(String stored) {
        Parsed parsed = parse(stored);
        SecretKey key = keyRing.keyFor(parsed.version())
                .orElseThrow(() -> new EncryptionException("Unknown key version: " + parsed.version()));
        byte[] payload = parsed.payload();
        if (payload.length < IV_BYTES + TAG_BITS / 8) {
            throw new EncryptionException("Encrypted value is too short");
        }
        try {
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, key,
                    Arrays.copyOfRange(payload, 0, IV_BYTES), parsed.version());
            byte[] plain = cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            // El tag no cuadra: el valor se ha manipulado, o se cifró con otra clave que tiene
            // la misma versión (clave mal rotada). En los dos casos, no se devuelve nada.
            throw new EncryptionException("Encrypted value failed authentication (tampered or wrong key)");
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }

    /**
     * {@code true} si el valor se cifró con una clave que ya no es la actual. Lo usará la
     * tarea de recifrado en segundo plano tras una rotación.
     */
    public boolean needsRotation(String stored) {
        return !parse(stored).version().equals(keyRing.currentVersion());
    }

    private static Cipher cipher(int mode, SecretKey key, byte[] iv, String version)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(version.getBytes(StandardCharsets.US_ASCII));
        return cipher;
    }

    private static Parsed parse(String stored) {
        if (stored == null) {
            throw new EncryptionException("Encrypted value is null");
        }
        int sep = stored.indexOf(SEPARATOR);
        if (sep <= 0) {
            throw new EncryptionException("Encrypted value has no version prefix");
        }
        try {
            return new Parsed(stored.substring(0, sep),
                    Base64.getDecoder().decode(stored.substring(sep + 1)));
        } catch (IllegalArgumentException e) {
            // Sin causa encadenada: el mensaje de Base64 puede citar parte del contenido.
            throw new EncryptionException("Encrypted value is not valid base64");
        }
    }

    private record Parsed(String version, byte[] payload) {
    }
}
