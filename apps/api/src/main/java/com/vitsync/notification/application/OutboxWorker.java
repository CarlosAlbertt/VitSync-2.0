package com.vitsync.notification.application;

import com.vitsync.notification.domain.OutboxMessage;
import com.vitsync.notification.infrastructure.EmailSender;
import com.vitsync.notification.infrastructure.OutboxRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envía los mensajes pendientes del outbox, con reintentos y backoff exponencial.
 *
 * <p>Cada mensaje se procesa en <b>su propia transacción</b>: reservar la fila
 * ({@code FOR UPDATE SKIP LOCKED}), enviar y marcar el resultado. Si un mensaje falla, el
 * resto del lote sigue.
 *
 * <p>Garantía: <b>al menos una vez</b>. Si el SMTP acepta el email pero la BD cae antes del
 * commit, el mensaje se reenviará. Para un código de verificación, un duplicado es
 * aceptable; perderlo, no.
 *
 * <p>Backoff: 30 s, 1 min, 2 min, 4 min... con tope de 1 h, y {@code FAILED} tras 8 intentos
 * (unas 2 horas en total). Lo bastante para un corte de SMTP normal, sin reintentar
 * eternamente un destinatario que no existe.
 *
 * <p>No se programa aquí: lo llama {@code OutboxScheduler}. Así los tests lo invocan a mano,
 * en el momento exacto, sin esperas.
 */
@Service
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    static final Duration BASE_BACKOFF = Duration.ofSeconds(30);
    static final Duration MAX_BACKOFF = Duration.ofHours(1);

    private final OutboxRepository outbox;
    private final EmailOutbox emails;
    private final EmailSender sender;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int maxAttempts;
    private final int batchSize;

    public OutboxWorker(OutboxRepository outbox, EmailOutbox emails, EmailSender sender,
                        TransactionTemplate tx, Clock clock,
                        @Value("${vitsync.outbox.max-attempts:8}") int maxAttempts,
                        @Value("${vitsync.outbox.batch-size:20}") int batchSize) {
        this.outbox = outbox;
        this.emails = emails;
        this.sender = sender;
        this.tx = tx;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
    }

    /** Procesa hasta {@code batchSize} mensajes vencidos. Devuelve cuántos ha intentado. */
    public int processBatch() {
        int processed = 0;
        while (processed < batchSize && Boolean.TRUE.equals(tx.execute(s -> processNext()))) {
            processed++;
        }
        return processed;
    }

    private boolean processNext() {
        Optional<OutboxMessage> next = outbox.claimNextDue(clock.instant());
        if (next.isEmpty()) {
            return false;
        }
        OutboxMessage message = next.get();
        try {
            EmailOutbox.EmailContent content = emails.open(message);
            sender.send(content.to(), content.template().subject(), content.template().render(content.variables()));
            message.markSent();
            log.info("Outbox message {} ({}) sent", message.getId(), message.getType());
        } catch (RuntimeException e) {
            // Solo el tipo de excepción: el mensaje de un error SMTP suele incluir el destinatario.
            String error = e.getClass().getName();
            message.markFailed(error, maxAttempts, backoffFor(message.getAttempts() + 1), clock.instant());
            log.warn("Outbox message {} ({}) failed, attempt {}/{}: {}", message.getId(), message.getType(),
                    message.getAttempts(), maxAttempts, error);
        }
        return true;
    }

    // 30 s × 2^(intento-1), con tope. El desplazamiento se limita para no desbordar.
    static Duration backoffFor(int attempt) {
        Duration delay = BASE_BACKOFF.multipliedBy(1L << Math.min(attempt - 1, 16));
        return delay.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : delay;
    }
}
