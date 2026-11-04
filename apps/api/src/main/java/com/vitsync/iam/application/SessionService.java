package com.vitsync.iam.application;

import com.vitsync.common.audit.AuditTrail;
import com.vitsync.common.error.BusinessException;
import com.vitsync.common.error.ErrorCode;
import com.vitsync.common.security.AccessTokenService;
import com.vitsync.iam.domain.Emails;
import com.vitsync.iam.domain.RefreshToken;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.domain.UserStatus;
import com.vitsync.iam.infrastructure.UserRepository;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Casos de uso de la sesión: login, refresh rotativo y logout (VIT-63, VIT-64).
 *
 * <p><b>Respuesta uniforme</b> (ADR-0004): email inexistente y contraseña incorrecta devuelven
 * el mismo {@code 401 INVALID_CREDENTIALS} <b>y tardan lo mismo</b>. Si el email no existe se
 * compara igualmente contra un hash ficticio: sin eso, la respuesta rápida (sin BCrypt)
 * delataría qué emails no tienen cuenta.
 *
 * <p>Solo después de comprobar la contraseña se dice si la cuenta está sin verificar o
 * desactivada: esa información ya no sirve para enumerar, porque exige conocer la contraseña.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokens;
    private final RefreshTokens refreshTokens;
    private final EmailVerificationService verification;
    private final AuditTrail audit;
    private final TransactionTemplate tx;
    private final Clock clock;
    // Hash de una contraseña que no es de nadie, con el mismo coste que los reales.
    private final String dummyHash;

    public SessionService(UserRepository users, PasswordEncoder passwordEncoder, AccessTokenService accessTokens,
                          RefreshTokens refreshTokens, EmailVerificationService verification,
                          AuditTrail audit, TransactionTemplate tx, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.verification = verification;
        this.audit = audit;
        this.tx = tx;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public Session login(String email, String password, String userAgent) {
        // Lectura y BCrypt fuera de transacción: el hash tarda ~250 ms y no necesita la BD.
        Optional<User> found = users.findByEmail(Emails.normalize(email));
        boolean matches = passwordEncoder.matches(password, found.map(User::getPasswordHash).orElse(dummyHash));
        if (found.isEmpty() || !matches) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            // Reenvío del código: así una cuenta cuyo código caducó no se queda bloqueada, y
            // no hace falta un endpoint de reenvío que permitiría enumerar emails.
            tx.executeWithoutResult(s -> verification.resendIfNeeded(users.getReferenceById(user.getId())));
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (!user.canLogIn()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        String refresh = tx.execute(s -> refreshTokens.startFamily(users.getReferenceById(user.getId()), userAgent));
        log.info("Login: {}", user.getPublicId());
        return new Session(accessTokens.issue(user.getPublicId(), user.getRole().name()).value(),
                refresh, MeView.of(user));
    }

    /**
     * Canjea un refresh token por una sesión nueva, <b>rotándolo</b> (ADR-0004): el token
     * presentado queda revocado y se emite otro de la <b>misma familia</b>.
     *
     * <p><b>Detección de reuso</b>: si llega un token que ya se canjeó, alguien lo está usando
     * por segunda vez. Puede ser el atacante que lo robó o el usuario legítimo después del
     * atacante; desde el servidor no se distinguen, así que se revoca <b>la familia entera</b>
     * (las dos sesiones caen) y queda en {@code audit_logs}. Es la señal de robo que v1 no
     * tenía.
     *
     * <p>Como en la verificación, la transacción se confirma <b>antes</b> de lanzar el 401: si
     * la excepción saliera de dentro, se desharía justo la revocación de la familia.
     */
    public Session refresh(String refreshToken, String userAgent) {
        Optional<Session> session = tx.execute(s -> refreshTokens.findForUpdate(refreshToken)
                .flatMap(token -> rotate(token, userAgent)));
        return session.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    private Optional<Session> rotate(RefreshToken token, String userAgent) {
        User user = token.getUser();
        if (token.wasConsumed()) {
            refreshTokens.revokeFamily(token.getFamilyId());
            audit.record("REFRESH_TOKEN_REUSE", user.getPublicId().toString(), "TOKEN_FAMILY",
                    token.getFamilyId().toString(), false, Map.of("reaction", "FAMILY_REVOKED"));
            log.warn("Refresh token reuse detected: family {} of {} revoked", token.getFamilyId(), user.getPublicId());
            return Optional.empty();
        }
        if (token.isRevoked() || token.isExpired(clock.instant())) {
            // Sesión cerrada (logout, familia ya revocada) o caducada: 401 sin más.
            return Optional.empty();
        }
        if (!user.canLogIn()) {
            // Cuenta suspendida o borrada tras el login: sus sesiones dejan de renovarse.
            refreshTokens.revokeFamily(token.getFamilyId());
            return Optional.empty();
        }
        token.consume(clock.instant());
        String next = refreshTokens.issue(user, token.getFamilyId(), userAgent);
        return Optional.of(new Session(accessTokens.issue(user.getPublicId(), user.getRole().name()).value(),
                next, MeView.of(user)));
    }

    /**
     * Cierra la sesión del dispositivo: revoca la familia entera del refresh presentado.
     * Idempotente y silencioso: con un token desconocido o ya revocado no pasa nada, porque
     * el resultado para el cliente es el mismo (sin sesión).
     */
    public void logout(String refreshToken) {
        tx.executeWithoutResult(s -> refreshTokens.find(refreshToken)
                .ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId())));
    }
}
