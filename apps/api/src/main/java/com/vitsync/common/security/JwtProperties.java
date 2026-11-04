package com.vitsync.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de los access tokens (ADR-0004), leída de {@code vitsync.jwt.*}.
 *
 * @param privateKey         clave privada RSA en PEM (PKCS#8), {@code JWT_PRIVATE_KEY}. Solo
 *                           la tiene la API: con ella se firma
 * @param publicKey          clave pública RSA en PEM (X.509), {@code JWT_PUBLIC_KEY}. Con ella
 *                           se verifica; se puede repartir sin riesgo
 * @param issuer             valor de {@code iss}; el decoder rechaza tokens de otro emisor
 * @param accessTokenTtl     vida del access token: 15 minutos
 * @param allowEphemeralKeys sin claves configuradas, genera un par en memoria al arrancar.
 *                           Solo en {@code local} y en tests: los tokens dejan de valer al
 *                           reiniciar, lo que en producción cerraría todas las sesiones
 */
@ConfigurationProperties("vitsync.jwt")
public record JwtProperties(
        String privateKey,
        String publicKey,
        @DefaultValue("vitsync-api") String issuer,
        @DefaultValue("15m") Duration accessTokenTtl,
        boolean allowEphemeralKeys) {

    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl
                + ", allowEphemeralKeys=" + allowEphemeralKeys + "]";
    }
}
