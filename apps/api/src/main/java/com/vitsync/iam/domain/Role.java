package com.vitsync.iam.domain;

/**
 * Rol del usuario. Es un atributo, no un tipo (ADR-0003): se guarda en {@code users.role},
 * que tiene un CHECK con estos mismos valores.
 */
public enum Role {
    PATIENT,
    DOCTOR,
    ADMIN
}
