package com.vitsync.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Código de un solo uso enviado por email: tabla {@code one_time_codes} (V2).
 *
 * <p>Guarda el <b>hash</b> del código, nunca el código. Las reglas (caducidad, intentos, un
 * solo uso) están aquí, en el dominio, para que ningún servicio pueda saltárselas.
 *
 * <p>Se referencia al usuario por id y no con {@code @ManyToOne}: el código no necesita
 * cargar el {@link User}, y así no hay relaciones perezosas que gestionar.
 */
@Entity
@Table(name = "one_time_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OneTimeCode {

    /** Resultado de presentar un código. */
    public enum Check { VALID, INVALID, EXHAUSTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private CodePurpose purpose;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "code_hash", nullable = false, length = 64, updatable = false)
    private String codeHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static OneTimeCode issue(Long userId, CodePurpose purpose, String codeHash, Instant now) {
        OneTimeCode c = new OneTimeCode();
        c.userId = Objects.requireNonNull(userId);
        c.purpose = Objects.requireNonNull(purpose);
        c.codeHash = Objects.requireNonNull(codeHash);
        c.expiresAt = now.plus(purpose.ttl());
        return c;
    }

    /**
     * Comprueba un intento. {@code matches} lo calcula el servicio (comparación en tiempo
     * constante del hash); aquí se aplican las reglas en este orden:
     * <ol>
     *   <li>Intentos agotados → {@link Check#EXHAUSTED}, <b>aunque el código sea correcto</b>:
     *       si no, el límite no frenaría a quien sigue probando.
     *   <li>Caducado o ya usado → {@link Check#INVALID}.
     *   <li>Incorrecto → suma un intento; si era el último, {@link Check#EXHAUSTED}.
     *   <li>Correcto → se consume y no vuelve a servir.
     * </ol>
     */
    public Check attempt(boolean matches, Instant now) {
        if (attempts >= purpose.maxAttempts()) {
            return Check.EXHAUSTED;
        }
        if (consumedAt != null || !now.isBefore(expiresAt)) {
            return Check.INVALID;
        }
        if (!matches) {
            attempts++;
            return attempts >= purpose.maxAttempts() ? Check.EXHAUSTED : Check.INVALID;
        }
        consumedAt = now;
        return Check.VALID;
    }

    /** Sin usar, sin caducar y con intentos restantes. */
    public boolean isUsable(Instant now) {
        return consumedAt == null && now.isBefore(expiresAt) && attempts < purpose.maxAttempts();
    }
}
