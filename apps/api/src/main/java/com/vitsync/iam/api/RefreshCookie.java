package com.vitsync.iam.api;

import com.vitsync.iam.domain.RefreshToken;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * La cookie que lleva el refresh token (ADR-0004).
 *
 * <ul>
 *   <li>{@code HttpOnly}: JavaScript no puede leerla, así que un XSS no la roba.
 *   <li>{@code Secure}: solo por HTTPS. Se desactiva en {@code local}
 *       ({@code vitsync.auth.refresh-cookie.secure=false}) porque allí no hay TLS.
 *   <li>{@code SameSite=Lax}: el navegador no la envía en peticiones POST desde otra web, que
 *       es lo que haría falta para un ataque CSRF contra {@code /auth/refresh}.
 *   <li>{@code Path=/api/v1/auth}: solo viaja a los endpoints de autenticación, no en cada
 *       petición de la API.
 * </ul>
 */
@Component
public class RefreshCookie {

    public static final String NAME = "vitsync_refresh";
    static final String PATH = "/api/v1/auth";

    private final boolean secure;

    RefreshCookie(@Value("${vitsync.auth.refresh-cookie.secure:true}") boolean secure) {
        this.secure = secure;
    }

    ResponseCookie issue(String token) {
        return base(token).maxAge(RefreshToken.TTL).build();
    }

    /** Borra la cookie: mismo nombre y ruta, vacía y caducada. */
    ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH);
    }
}
