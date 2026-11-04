package com.vitsync.iam.application;

import com.vitsync.common.crypto.BlindIndex;
import com.vitsync.iam.domain.CodePurpose;
import com.vitsync.iam.domain.OneTimeCode;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.infrastructure.OneTimeCodeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emisión y comprobación de códigos de un solo uso (verificación y recuperación).
 *
 * <ul>
 *   <li><b>6 dígitos con {@link SecureRandom}</b>: impredecible. {@code Random} no sirve
 *       porque su secuencia se deduce a partir de unos pocos valores.
 *   <li><b>Hash HMAC con el pepper del blind index</b> y separación de dominio
 *       ({@code otp:<propósito>:<usuario>:<código>}): el mismo código para otro usuario u
 *       otro propósito da otra huella. Con SHA-256 a secas, un millón de combinaciones se
 *       revierten en milisegundos con la BD robada; sin el pepper, no.
 *   <li><b>Comparación en tiempo constante</b> ({@link MessageDigest#isEqual}): una
 *       comparación normal termina antes cuanto antes difiere, y eso se puede medir.
 * </ul>
 *
 * <p>Uso interno del módulo: siempre dentro de la transacción del caso de uso que lo llama.
 */
@Component
class OneTimeCodes {

    private static final int DIGITS = 6;
    private static final int BOUND = 1_000_000;

    private final OneTimeCodeRepository codes;
    private final BlindIndex blindIndex;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    OneTimeCodes(OneTimeCodeRepository codes, BlindIndex blindIndex, Clock clock) {
        this.codes = codes;
        this.blindIndex = blindIndex;
        this.clock = clock;
    }

    /** Emite un código nuevo (invalida el anterior del mismo propósito) y lo devuelve en claro. */
    @Transactional(propagation = Propagation.MANDATORY)
    String issue(User user, CodePurpose purpose) {
        String code = String.format("%0" + DIGITS + "d", random.nextInt(BOUND));
        codes.deleteActive(user.getId(), purpose);
        codes.save(OneTimeCode.issue(user.getId(), purpose, hash(user, purpose, code), clock.instant()));
        return code;
    }

    /** Presenta un código. Los intentos fallidos quedan guardados al confirmar la transacción. */
    @Transactional(propagation = Propagation.MANDATORY)
    OneTimeCode.Check check(User user, CodePurpose purpose, String code) {
        return codes.findByUserIdAndPurposeAndConsumedAtIsNull(user.getId(), purpose)
                .map(stored -> stored.attempt(matches(stored, user, purpose, code), clock.instant()))
                .orElse(OneTimeCode.Check.INVALID);
    }

    /** ¿Hay un código vivo, sin caducar y con intentos restantes? */
    @Transactional(propagation = Propagation.MANDATORY)
    boolean hasUsableCode(User user, CodePurpose purpose) {
        return codes.findByUserIdAndPurposeAndConsumedAtIsNull(user.getId(), purpose)
                .filter(c -> c.isUsable(clock.instant()))
                .isPresent();
    }

    private boolean matches(OneTimeCode stored, User user, CodePurpose purpose, String code) {
        return MessageDigest.isEqual(
                stored.getCodeHash().getBytes(StandardCharsets.US_ASCII),
                hash(user, purpose, code).getBytes(StandardCharsets.US_ASCII));
    }

    private String hash(User user, CodePurpose purpose, String code) {
        return blindIndex.hash("otp:" + purpose + ":" + user.getPublicId() + ":" + code);
    }
}
