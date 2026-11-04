package com.vitsync.iam.application;

import com.vitsync.common.error.BusinessException;
import com.vitsync.common.error.ErrorCode;
import com.vitsync.iam.domain.CodePurpose;
import com.vitsync.iam.domain.Emails;
import com.vitsync.iam.domain.OneTimeCode;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.domain.UserStatus;
import com.vitsync.iam.infrastructure.UserRepository;
import com.vitsync.notification.application.EmailOutbox;
import com.vitsync.notification.application.EmailTemplate;
import java.time.Clock;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Caso de uso: verificar el email con el código recibido (VIT-62).
 *
 * <p>Respuestas indistinguibles a propósito: email inexistente, cuenta ya activa, código
 * caducado o código incorrecto devuelven el mismo {@code INVALID_OR_EXPIRED_CODE}. Si no,
 * este endpoint serviría para averiguar qué emails tienen cuenta.
 *
 * <p>Por qué la transacción termina <b>antes</b> de lanzar el error: un intento fallido tiene
 * que quedar guardado. Si la excepción saliera de dentro, Spring desharía la transacción y
 * el contador de intentos no subiría nunca, así que el límite no frenaría nada.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final UserRepository users;
    private final OneTimeCodes codes;
    private final EmailOutbox outbox;
    private final TransactionTemplate tx;
    private final Clock clock;

    public EmailVerificationService(UserRepository users, OneTimeCodes codes, EmailOutbox outbox,
                                    TransactionTemplate tx, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.outbox = outbox;
        this.tx = tx;
        this.clock = clock;
    }

    public void verify(String email, String code) {
        OneTimeCode.Check result = tx.execute(s -> users.findByEmail(Emails.normalize(email))
                .filter(u -> u.getStatus() == UserStatus.PENDING_VERIFICATION)
                .map(user -> {
                    OneTimeCode.Check check = codes.check(user, CodePurpose.EMAIL_VERIFICATION, code);
                    if (check == OneTimeCode.Check.VALID) {
                        user.activate(clock.instant());
                        log.info("Email verified: {}", user.getPublicId());
                    }
                    return check;
                })
                .orElse(OneTimeCode.Check.INVALID));

        switch (result) {
            case VALID -> { }
            case EXHAUSTED -> throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
            case INVALID -> throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_CODE);
        }
    }

    /**
     * Emite un código de verificación y encola el email, en la transacción de quien llama
     * (el alta, o un login de una cuenta sin verificar).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sendCode(User user) {
        String code = codes.issue(user, CodePurpose.EMAIL_VERIFICATION);
        outbox.enqueue(EmailTemplate.VERIFY_EMAIL, user.getEmail(), Map.of(
                "firstName", user.getFirstName(),
                "code", code,
                "minutes", String.valueOf(CodePurpose.EMAIL_VERIFICATION.ttl().toMinutes())));
    }

    /**
     * Reenvía el código solo si no queda uno utilizable: así repetir el login no inunda de
     * emails al usuario, pero un código caducado o agotado se repone.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void resendIfNeeded(User user) {
        if (!codes.hasUsableCode(user, CodePurpose.EMAIL_VERIFICATION)) {
            sendCode(user);
        }
    }
}
