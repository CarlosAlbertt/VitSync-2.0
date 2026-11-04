package com.vitsync.iam.domain;

/**
 * Ciclo de vida de una cuenta. Mismos valores que el CHECK de {@code users.status}.
 *
 * <p>Una cuenta nace en {@link #PENDING_VERIFICATION} y pasa a {@link #ACTIVE} al verificar
 * el email. Solo {@link #ACTIVE} puede iniciar sesión.
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED,
    ANONYMIZED
}
