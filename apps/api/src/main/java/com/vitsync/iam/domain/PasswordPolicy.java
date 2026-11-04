package com.vitsync.iam.domain;

import java.nio.charset.StandardCharsets;

/**
 * Política de contraseñas (ADR-0004 y NIST SP 800-63B).
 *
 * <ul>
 *   <li><b>Mínimo 12 caracteres</b>. Sin reglas de composición ("una mayúscula, un
 *       símbolo..."): NIST las desaconseja porque empujan a patrones previsibles como
 *       {@code Password1!}. La longitud y la lista de filtradas protegen más.
 *   <li><b>Máximo 72 bytes en UTF-8</b>. Es el límite de BCrypt: lo que pasa de ahí se
 *       ignoraría (o, en Spring Security 7, lanza un error). Se cuenta en bytes, no en
 *       caracteres, porque una tilde o un emoji ocupan más de uno.
 * </ul>
 *
 * <p>La comprobación contra HaveIBeenPwned no está aquí: necesita red y vive en el servicio.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean isAcceptable(String password) {
        if (password == null) {
            return false;
        }
        // codePointCount: un emoji cuenta como un carácter, no como dos "char" de Java.
        return password.codePointCount(0, password.length()) >= MIN_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
