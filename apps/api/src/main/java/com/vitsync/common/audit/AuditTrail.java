package com.vitsync.common.audit;

import com.vitsync.common.web.TraceIdFilter;
import java.time.Clock;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registro de eventos de seguridad en {@code audit_logs}.
 *
 * <p>Vive en {@code common} porque lo usarán todos los módulos (login, acceso a informes,
 * RGPD...). El {@code traceId} de la petición se copia del MDC: con él, una entrada de
 * auditoría lleva directamente a las líneas de log de esa petición.
 *
 * <p>{@code Propagation.MANDATORY}: el evento se guarda en la transacción del cambio que
 * registra. Si la revocación de una familia se deshiciera, tampoco quedaría constancia de una
 * revocación que no ocurrió.
 */
@Service
public class AuditTrail {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AuditLogRepository logs;
    private final Clock clock;

    AuditTrail(AuditLogRepository logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    /**
     * @param details solo referencias opacas y hechos; nunca datos personales ni clínicos
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String actorRef, String targetType, String targetRef,
                       boolean success, Map<String, String> details) {
        String json = details == null || details.isEmpty() ? null : JSON.writeValueAsString(details);
        logs.save(AuditLog.of(action, actorRef, targetType, targetRef, success,
                MDC.get(TraceIdFilter.MDC_KEY), json, clock.instant()));
    }
}
