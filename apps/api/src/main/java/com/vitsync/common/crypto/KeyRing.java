package com.vitsync.common.crypto;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Juego de claves de cifrado: la actual, para cifrar, y las anteriores, para descifrar.
 *
 * <p>Se valida entero en el constructor, que Spring ejecuta al arrancar. Si falta una clave,
 * no mide 32 bytes o la versión no tiene el formato {@code v<n>}, se lanza
 * {@link EncryptionException} y <b>la aplicación no arranca</b> (ADR-0005). Es preferible a
 * descubrir en producción, con el primer registro, que la clave estaba mal copiada.
 *
 * <p>Por qué existen las claves anteriores: cada valor cifrado lleva delante la versión de
 * la clave que lo cifró ({@code v1:...}). Al rotar, la clave nueva pasa a ser la actual y la
 * vieja se mueve a {@code previous-keys}; así lo ya guardado se sigue leyendo mientras se
 * recifra en segundo plano. Sin este mecanismo, cambiar la clave dejaría ilegibles todos los
 * datos (el fallo de diseño de v1).
 */
public final class KeyRing {

    /** AES-256: la clave mide 256 bits. */
    static final int KEY_BYTES = 32;

    private static final Pattern VERSION = Pattern.compile("^v\\d+$");
    private static final String AES = "AES";

    private final String currentVersion;
    private final Map<String, SecretKey> keys;

    public KeyRing(CryptoProperties props) {
        this.currentVersion = requireVersion(props.keyVersion(), "vitsync.crypto.key-version (ENCRYPTION_KEY_VERSION)");

        Map<String, SecretKey> all = new HashMap<>();
        all.put(currentVersion, decodeKey(props.key(), "vitsync.crypto.key (ENCRYPTION_KEY)",
                props.allowInsecureDevKey()));

        props.previousKeys().forEach((version, encoded) -> {
            String name = "vitsync.crypto.previous-keys." + version;
            requireVersion(version, name);
            // Si la misma versión apareciera como actual y como anterior, no sabríamos con
            // cuál descifrar. Es un error de configuración y se corta aquí.
            if (version.equals(currentVersion)) {
                throw new EncryptionException(name + " duplicates the current key version");
            }
            all.put(version, decodeKey(encoded, name, props.allowInsecureDevKey()));
        });
        this.keys = Map.copyOf(all);
    }

    /** Versión con la que se cifra todo lo nuevo, p. ej. {@code v2}. */
    public String currentVersion() {
        return currentVersion;
    }

    SecretKey currentKey() {
        return keys.get(currentVersion);
    }

    /** Clave de una versión concreta; vacío si la versión no se conoce. */
    Optional<SecretKey> keyFor(String version) {
        return Optional.ofNullable(keys.get(version));
    }

    private static String requireVersion(String version, String name) {
        if (version == null || !VERSION.matcher(version).matches()) {
            // La versión no es secreta, pero tampoco hace falta repetirla: basta con el nombre.
            throw new EncryptionException(name + " must match v<number>, e.g. v1");
        }
        return version;
    }

    // Decodifica y comprueba una clave. Los mensajes dicen QUÉ propiedad falla, nunca su valor.
    private static SecretKey decodeKey(String encoded, String name, boolean allowInsecureDevKey) {
        if (encoded == null || encoded.isBlank()) {
            throw new EncryptionException(name + " is required");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(encoded.strip());
        } catch (IllegalArgumentException e) {
            // No se encadena la causa: el mensaje de la excepción de Base64 puede citar
            // caracteres de la clave.
            throw new EncryptionException(name + " is not valid base64");
        }
        try {
            if (raw.length != KEY_BYTES) {
                throw new EncryptionException(name + " must decode to " + KEY_BYTES + " bytes");
            }
            // La clave de desarrollo es 32 bytes a cero: así puede estar en el repositorio sin
            // que gitleaks salte y sin que nadie la confunda con un secreto. Pero si llegara a
            // producción, cifraría con una clave pública. Por eso se rechaza salvo que el
            // perfil la permita de forma explícita.
            if (isAllZero(raw) && !allowInsecureDevKey) {
                throw new EncryptionException(name + " is the insecure development key; "
                        + "set a real key or vitsync.crypto.allow-insecure-dev-key=true (local/test only)");
            }
            return new SecretKeySpec(raw, AES);
        } finally {
            // SecretKeySpec copia el array: el original se borra para no dejar la clave
            // suelta en memoria más tiempo del necesario.
            Arrays.fill(raw, (byte) 0);
        }
    }

    private static boolean isAllZero(byte[] bytes) {
        int acc = 0;
        for (byte b : bytes) {
            acc |= b;
        }
        return acc == 0;
    }

    @Override
    public String toString() {
        return "KeyRing[current=" + currentVersion + ", versions=" + keys.keySet() + "]";
    }
}
