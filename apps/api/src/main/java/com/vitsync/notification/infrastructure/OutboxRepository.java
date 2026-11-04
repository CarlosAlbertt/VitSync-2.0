package com.vitsync.notification.infrastructure;

import com.vitsync.notification.domain.OutboxMessage;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * Reserva el siguiente mensaje que toca enviar.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED}: bloquea la fila hasta el final de la transacción y
     * se salta las que ya tiene bloqueadas otra. Con dos instancias de la API (o dos hilos),
     * cada una coge un mensaje distinto y ninguno se envía dos veces a la vez. Es la forma
     * estándar de usar PostgreSQL como cola sin un broker aparte.
     */
    @Query(value = """
            SELECT * FROM outbox_messages
             WHERE status = 'PENDING' AND next_attempt_at <= :now
             ORDER BY next_attempt_at
             LIMIT 1
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxMessage> claimNextDue(@Param("now") Instant now);
}
