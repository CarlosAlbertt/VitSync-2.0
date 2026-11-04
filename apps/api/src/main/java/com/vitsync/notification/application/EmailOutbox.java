package com.vitsync.notification.application;

import com.vitsync.common.crypto.FieldEncryptor;
import com.vitsync.notification.domain.OutboxMessage;
import com.vitsync.notification.infrastructure.OutboxRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Puerta pública del módulo {@code notification} para encolar emails.
 *
 * <p>{@code Propagation.MANDATORY}: <b>exige</b> una transacción abierta por quien llama, y
 * falla si no la hay. Es la garantía del patrón outbox: el email se guarda en la misma
 * transacción que el cambio que lo provoca (el alta, la petición de recuperación...). Sin
 * esto, alguien podría encolar fuera de la transacción y volver al problema de partida.
 *
 * <p>El payload guardado es {@code {"data": "v1:..."}}: el JSON con destinatario y variables
 * va cifrado con {@link FieldEncryptor}, porque contiene un código de un solo uso vigente.
 */
@Service
public class EmailOutbox {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final OutboxRepository outbox;
    private final FieldEncryptor encryptor;
    private final Clock clock;

    public EmailOutbox(OutboxRepository outbox, FieldEncryptor encryptor, Clock clock) {
        this.outbox = outbox;
        this.encryptor = encryptor;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(EmailTemplate template, String to, Map<String, String> variables) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("to", to);
        content.put("variables", variables);
        String encrypted = encryptor.encrypt(JSON.writeValueAsString(content));
        outbox.save(OutboxMessage.pending(template.name(),
                JSON.writeValueAsString(Map.of("data", encrypted)), clock.instant()));
    }

    /** Lo inverso de {@link #enqueue}: lo usa el worker para enviar. */
    EmailContent open(OutboxMessage message) {
        String encrypted = JSON.readTree(message.getPayload()).get("data").asString();
        var content = JSON.readTree(encryptor.decrypt(encrypted));
        Map<String, String> variables = new LinkedHashMap<>();
        content.get("variables").properties().forEach(e -> variables.put(e.getKey(), e.getValue().asString()));
        return new EmailContent(EmailTemplate.valueOf(message.getType()), content.get("to").asString(), variables);
    }

    record EmailContent(EmailTemplate template, String to, Map<String, String> variables) {
    }
}
