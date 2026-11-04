package com.vitsync.common.crypto;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del cifrado en reposo (ADR-0005), leída de {@code vitsync.crypto.*}.
 *
 * <p>Es un record: Spring la rellena por constructor y queda inmutable. Los valores llegan
 * del entorno ({@code ENCRYPTION_KEY}, {@code ENCRYPTION_KEY_VERSION},
 * {@code BLIND_INDEX_PEPPER}); aquí no se valida nada, eso lo hace {@link KeyRing} al
 * arrancar para que la aplicación no llegue a levantarse con una clave mal puesta.
 *
 * @param key                 clave actual, 32 bytes en base64. Con ella se cifra todo lo nuevo
 * @param keyVersion          versión de la clave actual, con formato {@code v<n>} (p. ej. {@code v2})
 * @param blindIndexPepper    secreto del HMAC del blind index; distinto de la clave de cifrado
 * @param previousKeys        claves retiradas, versión → clave en base64. Solo sirven para
 *                            descifrar lo que se guardó antes de rotar. En el entorno:
 *                            {@code VITSYNC_CRYPTO_PREVIOUS_KEYS_V1=...}
 * @param allowInsecureDevKey permite la clave de desarrollo (32 bytes a cero). Solo en el
 *                            perfil {@code local} y en los tests; en producción, nunca
 */
@ConfigurationProperties("vitsync.crypto")
public record CryptoProperties(
        String key,
        String keyVersion,
        String blindIndexPepper,
        Map<String, String> previousKeys,
        boolean allowInsecureDevKey) {

    public CryptoProperties {
        // Sin claves anteriores, mapa vacío en vez de null: ahorra comprobaciones después.
        previousKeys = previousKeys == null ? Map.of() : Map.copyOf(previousKeys);
    }

    // Un record genera toString() con todos sus campos, y un toString() acaba tarde o
    // temprano en un log o en un volcado de depuración. Se sobrescribe para que las claves
    // no salgan nunca por ahí.
    @Override
    public String toString() {
        return "CryptoProperties[keyVersion=" + keyVersion
                + ", previousKeyVersions=" + previousKeys.keySet()
                + ", allowInsecureDevKey=" + allowInsecureDevKey + "]";
    }
}
