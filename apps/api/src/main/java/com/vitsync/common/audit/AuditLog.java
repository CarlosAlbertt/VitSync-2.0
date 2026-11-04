package com.vitsync.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Una entrada del registro de auditoría: tabla {@code audit_logs}.
 *
 * <p>Append-only: no tiene métodos para cambiarla, solo para crearla. Sin FK al usuario a
 * propósito (V1): la traza sobrevive a la anonimización RGPD; por eso {@code actorRef} es un
 * texto ({@code public_id}), no una relación.
 *
 * <p>Prohibido (ADR-0005): datos clínicos o personales en {@code details}. Solo referencias
 * opacas (ids) y hechos.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    private static final int TRACE_ID_MAX = 32;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_ref", length = 64, updatable = false)
    private String actorRef;

    @Column(nullable = false, length = 48, updatable = false)
    private String action;

    @Column(name = "target_type", length = 48, updatable = false)
    private String targetType;

    @Column(name = "target_ref", length = 64, updatable = false)
    private String targetRef;

    @Column(nullable = false, updatable = false)
    private boolean success;

    @Column(name = "trace_id", length = TRACE_ID_MAX, updatable = false)
    private String traceId;

    @Column(columnDefinition = "jsonb", updatable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    static AuditLog of(String action, String actorRef, String targetType, String targetRef,
                       boolean success, String traceId, String detailsJson, Instant now) {
        AuditLog a = new AuditLog();
        a.action = action;
        a.actorRef = actorRef;
        a.targetType = targetType;
        a.targetRef = targetRef;
        a.success = success;
        // El X-Request-Id entrante admite hasta 64 caracteres y la columna 32: se recorta.
        a.traceId = traceId == null || traceId.length() <= TRACE_ID_MAX ? traceId : traceId.substring(0, TRACE_ID_MAX);
        a.details = detailsJson;
        a.occurredAt = now;
        return a;
    }
}
