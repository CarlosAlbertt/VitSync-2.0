package com.vitsync.iam.domain;

import java.time.Duration;

/**
 * Para qué sirve un código de un solo uso, con su caducidad y su límite de intentos.
 *
 * <p>Los valores salen del backlog: verificación 10 minutos y 10 intentos (VIT-62);
 * recuperación 15 minutos (VIT-65), con menos intentos porque da acceso a la cuenta.
 * Con un código de 6 dígitos y 10 intentos, adivinarlo tiene una probabilidad de 1 entre
 * 100 000.
 */
public enum CodePurpose {

    EMAIL_VERIFICATION(Duration.ofMinutes(10), 10),
    PASSWORD_RESET(Duration.ofMinutes(15), 5);

    private final Duration ttl;
    private final int maxAttempts;

    CodePurpose(Duration ttl, int maxAttempts) {
        this.ttl = ttl;
        this.maxAttempts = maxAttempts;
    }

    public Duration ttl() {
        return ttl;
    }

    public int maxAttempts() {
        return maxAttempts;
    }
}
