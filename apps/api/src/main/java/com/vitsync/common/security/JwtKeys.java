package com.vitsync.common.security;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Par de claves RSA con el que se firman y verifican los access tokens.
 *
 * <p>Se valida al arrancar, como las claves de cifrado: si falta una, no es RSA, mide menos de
 * 2048 bits o la pública no corresponde a la privada, la aplicación <b>no arranca</b>. Es
 * mejor que descubrirlo cuando ningún token valide.
 *
 * <p>No hay claves de desarrollo en el repositorio: un PEM privado haría saltar a gitleaks (y
 * con razón). En {@code local} y en los tests se genera un par efímero en memoria.
 */
public final class JwtKeys {

    private static final Logger log = LoggerFactory.getLogger(JwtKeys.class);
    static final int MIN_BITS = 2048;

    private final RSAPublicKey publicKey;
    private final RSAPrivateCrtKey privateKey;

    private JwtKeys(RSAPublicKey publicKey, RSAPrivateCrtKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public static JwtKeys from(JwtProperties props) {
        boolean missing = isBlank(props.privateKey()) || isBlank(props.publicKey());
        if (missing) {
            if (!props.allowEphemeralKeys()) {
                throw new IllegalStateException("vitsync.jwt.private-key (JWT_PRIVATE_KEY) and "
                        + "vitsync.jwt.public-key (JWT_PUBLIC_KEY) are required");
            }
            log.warn("No JWT keys configured: using an ephemeral RSA key pair (local/test only); "
                    + "tokens will not survive a restart");
            return ephemeral();
        }
        try {
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey priv = (RSAPrivateCrtKey) rsa.generatePrivate(
                    new PKCS8EncodedKeySpec(pemBody(props.privateKey(), "PRIVATE KEY")));
            RSAPublicKey pub = (RSAPublicKey) rsa.generatePublic(
                    new X509EncodedKeySpec(pemBody(props.publicKey(), "PUBLIC KEY")));
            return validated(pub, priv);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // Sin causa encadenada ni contenido: el mensaje del parser podría citar la clave.
            throw new IllegalStateException("JWT keys are not valid RSA keys in PEM "
                    + "(PKCS#8 private key, X.509 public key)");
        }
    }

    static JwtKeys ephemeral() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(MIN_BITS);
            KeyPair pair = generator.generateKeyPair();
            return new JwtKeys((RSAPublicKey) pair.getPublic(), (RSAPrivateCrtKey) pair.getPrivate());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA not available", e);
        }
    }

    private static JwtKeys validated(RSAPublicKey pub, RSAPrivateCrtKey priv) {
        if (pub.getModulus().bitLength() < MIN_BITS) {
            throw new IllegalStateException("JWT keys must be at least " + MIN_BITS + " bits");
        }
        // Mismo módulo y mismo exponente público: la pública es la de esta privada. Si no,
        // la API firmaría tokens que ella misma rechazaría.
        if (!pub.getModulus().equals(priv.getModulus()) || !pub.getPublicExponent().equals(priv.getPublicExponent())) {
            throw new IllegalStateException("JWT public key does not match the private key");
        }
        return new JwtKeys(pub, priv);
    }

    // Quita la cabecera "-----BEGIN ...-----", el pie y los saltos de línea. Admite los "\n"
    // escritos literalmente, que es como suele llegar un PEM en una variable de entorno.
    private static byte[] pemBody(String pem, String type) {
        String body = pem.replace("\\n", "\n")
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    RSAPrivateCrtKey privateKey() {
        return privateKey;
    }

    /** Identificador corto de la clave ({@code kid}): permite rotarla más adelante. */
    String keyId() {
        return publicKey.getModulus().mod(BigInteger.valueOf(Long.MAX_VALUE)).toString(36);
    }

    @Override
    public String toString() {
        return "JwtKeys[kid=" + keyId() + "]";
    }
}
