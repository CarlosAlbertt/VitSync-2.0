package com.vitsync.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Un mensaje pendiente de salir del sistema (de momento, emails): tabla {@code outbox_messages}.
 *
 * <p><b>Patrón outbox transaccional</b>: quien necesita enviar un email no lo envía; inserta
 * esta fila en <b>su misma transacción</b>. Si la transacción se confirma, el mensaje existe;
 * si se deshace, tampoco existe. Después un worker lo envía con reintentos. Así un SMTP caído
 * no tumba el registro, y un registro fallido no deja un email enviado (fallo P9 de v1, que
 * enviaba directo desde el servicio).
 *
 * <p>El {@code payload} es JSON, pero su contenido va <b>cifrado</b>: lleva el destinatario y
 * el código de un solo uso, y no deben poder leerse en un volcado de la BD.
 */
@Entity
@Table(name = "outbox_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String type;

    // jsonb: el driver envía texto, y "?::jsonb" le dice a PostgreSQL que lo convierta. Así
    // no hace falta un tipo JSON de Hibernate ni su integración con Jackson.
    @Column(nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    // smallint en la BD: short para que la validación del esquema cuadre.
    @Column(nullable = false)
    private short attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static OutboxMessage pending(String type, String payloadJson, Instant now) {
        OutboxMessage m = new OutboxMessage();
        m.type = Objects.requireNonNull(type);
        m.payload = Objects.requireNonNull(payloadJson);
        m.status = OutboxStatus.PENDING;
        m.nextAttemptAt = now;
        return m;
    }

    public void markSent() {
        status = OutboxStatus.SENT;
        lastError = null;
    }

    /**
     * Registra un fallo. Con reintentos restantes, vuelve a la cola tras {@code backoff};
     * si no, queda en {@link OutboxStatus#FAILED}.
     *
     * @param error tipo de error, nunca el mensaje completo: un error SMTP suele citar el
     *              destinatario, y aquí no debe acabar ningún dato personal
     */
    public void markFailed(String error, int maxAttempts, Duration backoff, Instant now) {
        attempts++;
        lastError = error;
        if (attempts >= maxAttempts) {
            status = OutboxStatus.FAILED;
        } else {
            nextAttemptAt = now.plus(backoff);
        }
    }
}
