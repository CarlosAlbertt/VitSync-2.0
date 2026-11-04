package com.vitsync.iam.application;

import com.vitsync.common.audit.AuditTrail;
import com.vitsync.common.error.BusinessException;
import com.vitsync.common.error.ErrorCode;
import com.vitsync.iam.domain.CodePurpose;
import com.vitsync.iam.domain.Emails;
import com.vitsync.iam.domain.OneTimeCode;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.infrastructure.BreachedPasswordChecker;
import com.vitsync.iam.infrastructure.UserRepository;
import com.vitsync.notification.application.EmailOutbox;
import com.vitsync.notification.application.EmailTemplate;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Caso de uso: recuperación de contraseña por código de un solo uso enviado por email
 * (VIT-65). Sustituye a las preguntas de seguridad de v1, que NIST SP 800-63B desaconseja
 * (decisión 3 del plan del Sprint 1).
 *
 * <ol>
 *   <li><b>Pedir el código</b>: la respuesta es <b>idéntica</b> exista o no la cuenta. Si
 *       existe y está activa, se emite el código y se encola el email en la misma transacción.
 *   <li><b>Usar el código</b>: con el código correcto se cambia la contraseña y se
 *       <b>revocan todas las sesiones</b> (todas las familias de refresh): si alguien entró
 *       con la contraseña vieja, se queda fuera. Queda registrado en {@code audit_logs}.
 * </ol>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    // Pedirlo varias veces seguidas no manda varios emails: protege el buzón del usuario de
    // quien quiera inundarlo con peticiones a su nombre.
    static final Duration RESEND_WINDOW = Duration.ofMinutes(1);

    private final UserRepository users;
    private final OneTimeCodes codes;
    private final EmailOutbox outbox;
    private final RefreshTokens refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final BreachedPasswordChecker breachedPasswords;
    private final AuditTrail audit;
    private final TransactionTemplate tx;

    public PasswordResetService(UserRepository users, OneTimeCodes codes, EmailOutbox outbox,
                                RefreshTokens refreshTokens, PasswordEncoder passwordEncoder,
                                BreachedPasswordChecker breachedPasswords, AuditTrail audit,
                                TransactionTemplate tx) {
        this.users = users;
        this.codes = codes;
        this.outbox = outbox;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.breachedPasswords = breachedPasswords;
        this.audit = audit;
        this.tx = tx;
    }

    /** Siempre termina igual para quien llama: no devuelve nada ni lanza por "no existe". */
    public void requestCode(String email) {
        tx.executeWithoutResult(s -> users.findByEmail(Emails.normalize(email))
                .filter(User::canLogIn)
                .filter(u -> !codes.issuedRecently(u, CodePurpose.PASSWORD_RESET, RESEND_WINDOW))
                .ifPresent(this::sendCode));
    }

    public void reset(String email, String code, String newPassword) {
        if (breachedPasswords.isBreached(newPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_BREACHED);
        }
        // Hash antes de saber si el código vale: así el tiempo de respuesta no depende de
        // ello, y la transacción no espera a BCrypt.
        String newHash = passwordEncoder.encode(newPassword);

        OneTimeCode.Check result = tx.execute(s -> users.findByEmail(Emails.normalize(email))
                .filter(User::canLogIn)
                .map(user -> {
                    OneTimeCode.Check check = codes.check(user, CodePurpose.PASSWORD_RESET, code);
                    if (check == OneTimeCode.Check.VALID) {
                        user.changePassword(newHash);
                        refreshTokens.revokeAllFor(user);
                        audit.record("PASSWORD_RESET", user.getPublicId().toString(), "USER",
                                user.getPublicId().toString(), true, Map.of("sessions", "REVOKED"));
                        log.info("Password reset: {}", user.getPublicId());
                    }
                    return check;
                })
                .orElse(OneTimeCode.Check.INVALID));

        // Fuera de la transacción, como en la verificación: los intentos fallidos se guardan.
        switch (result) {
            case VALID -> { }
            case EXHAUSTED -> throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
            case INVALID -> throw new BusinessException(ErrorCode.INVALID_OR_EXPIRED_CODE);
        }
    }

    private void sendCode(User user) {
        String code = codes.issue(user, CodePurpose.PASSWORD_RESET);
        outbox.enqueue(EmailTemplate.PASSWORD_RESET, user.getEmail(), Map.of(
                "firstName", user.getFirstName(),
                "code", code,
                "minutes", String.valueOf(CodePurpose.PASSWORD_RESET.ttl().toMinutes())));
    }
}
