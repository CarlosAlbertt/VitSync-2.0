package com.vitsync.iam.application;

import com.vitsync.iam.domain.RefreshToken;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.infrastructure.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emisión y búsqueda de refresh tokens opacos (ADR-0004). Uso interno del módulo.
 *
 * <p>El token son 32 bytes de {@link SecureRandom} en base64url (43 caracteres, válidos en
 * una cookie sin escapar). Se devuelve en claro una sola vez y se guarda su SHA-256.
 */
@Component
class RefreshTokens {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository tokens;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    RefreshTokens(RefreshTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /** Token de una familia nueva: un login. */
    @Transactional(propagation = Propagation.MANDATORY)
    String startFamily(User user, String userAgent) {
        return issue(user, UUID.randomUUID(), userAgent);
    }

    /** Token de una familia existente: una rotación. */
    @Transactional(propagation = Propagation.MANDATORY)
    String issue(User user, UUID familyId, String userAgent) {
        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(RefreshToken.issue(user, hash(token), familyId, userAgent, clock.instant()));
        return token;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Optional<RefreshToken> find(String token) {
        return token == null || token.isBlank() ? Optional.empty() : tokens.findByTokenHash(hash(token));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void revokeFamily(UUID familyId) {
        tokens.revokeFamily(familyId, clock.instant());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void revokeAllFor(User user) {
        tokens.revokeAllForUser(user.getId(), clock.instant());
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
