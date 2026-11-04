package com.vitsync.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Refresh token opaco (ADR-0004): tabla {@code refresh_tokens}.
 *
 * <p>Se guarda <b>solo el SHA-256</b> del token. El valor viaja una única vez, en la cookie
 * {@code HttpOnly}; con la BD robada no se obtiene ningún token usable. SHA-256 sin sal basta
 * aquí (a diferencia de una contraseña): el token son 32 bytes aleatorios, imposibles de
 * adivinar por fuerza bruta.
 *
 * <p>{@code familyId}: todos los tokens que salen de un mismo login comparten familia. Es lo
 * que permite cortar una sesión robada entera (detección de reuso, PR siguiente).
 *
 * <p>{@code ip_address} (tipo {@code inet}) no se mapea todavía: no se usa, y Hibernate solo
 * valida las columnas mapeadas.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    public static final Duration TTL = Duration.ofDays(7);
    private static final int MAX_USER_AGENT = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent", length = MAX_USER_AGENT)
    private String userAgent;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static RefreshToken issue(User user, String tokenHash, UUID familyId, String userAgent, Instant now) {
        RefreshToken t = new RefreshToken();
        t.user = Objects.requireNonNull(user);
        t.tokenHash = Objects.requireNonNull(tokenHash);
        t.familyId = Objects.requireNonNull(familyId);
        t.expiresAt = now.plus(TTL);
        t.userAgent = userAgent == null || userAgent.length() <= MAX_USER_AGENT
                ? userAgent : userAgent.substring(0, MAX_USER_AGENT);
        return t;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
