package com.vitsync.notification.domain;

/** Estados de un mensaje del outbox. Mismos valores que el CHECK de V2. */
public enum OutboxStatus {
    /** Pendiente de enviar (también tras un fallo, hasta agotar los reintentos). */
    PENDING,
    SENT,
    /** Agotó los reintentos. Queda para revisión manual; no se borra. */
    FAILED
}
