package com.vitsync.iam.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Consulta la API "Pwned Passwords" de HaveIBeenPwned con <b>k-anonimato</b>.
 *
 * <p>Cómo funciona, y por qué la contraseña no sale nunca del servidor:
 * <ol>
 *   <li>Se calcula el SHA-1 de la contraseña, p. ej. {@code 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8}.
 *   <li>Solo se envían los <b>5 primeros caracteres</b> ({@code 5BAA6}). Cada prefijo lo
 *       comparten cientos de hashes, así que el servicio no sabe cuál es el nuestro.
 *   <li>Responde con todos los sufijos que empiezan por ese prefijo, y el que buscamos se
 *       compara aquí, en local.
 * </ol>
 * La cabecera {@code Add-Padding} pide respuestas rellenadas con entradas falsas (con
 * recuento 0) para que ni siquiera el tamaño de la respuesta delate el prefijo.
 *
 * <p>SHA-1 no se usa aquí como protección (para eso está BCrypt): es solo el formato del
 * índice de HaveIBeenPwned.
 *
 * <p><b>Fallo abierto</b>: cualquier error (timeout, 5xx, red) devuelve {@code false} con un
 * aviso en el log. Un tercero caído no puede impedir que la gente se registre.
 */
public class PwnedPasswordsClient implements BreachedPasswordChecker {

    private static final Logger log = LoggerFactory.getLogger(PwnedPasswordsClient.class);
    private static final int PREFIX_LENGTH = 5;

    private final RestClient http;

    public PwnedPasswordsClient(RestClient http) {
        this.http = http;
    }

    @Override
    public boolean isBreached(String password) {
        String sha1 = sha1Hex(password);
        String prefix = sha1.substring(0, PREFIX_LENGTH);
        String suffix = sha1.substring(PREFIX_LENGTH);
        try {
            String body = http.get()
                    .uri("/range/{prefix}", prefix)
                    .header("Add-Padding", "true")
                    .retrieve()
                    .body(String.class);
            return body != null && containsSuffix(body, suffix);
        } catch (RuntimeException e) {
            // Solo el tipo de error: ni la contraseña ni el hash llegan al log.
            log.warn("Pwned Passwords check skipped: service unavailable ({})", e.getClass().getSimpleName());
            return false;
        }
    }

    // Cada línea es "SUFIJO:RECUENTO". Las de relleno traen recuento 0 y no cuentan.
    private static boolean containsSuffix(String body, String suffix) {
        return body.lines().anyMatch(line -> {
            int colon = line.indexOf(':');
            return colon > 0
                    && line.substring(0, colon).trim().equalsIgnoreCase(suffix)
                    && !line.substring(colon + 1).trim().equals("0");
        });
    }

    private static String sha1Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            // Todas las JVM traen SHA-1: si falta, el entorno está roto.
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
