package com.vitsync.iam.application;

import com.vitsync.common.crypto.BlindIndex;
import com.vitsync.common.error.BusinessException;
import com.vitsync.common.error.ErrorCode;
import com.vitsync.iam.domain.Emails;
import com.vitsync.iam.domain.Nif;
import com.vitsync.iam.domain.PatientProfile;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.infrastructure.BreachedPasswordChecker;
import com.vitsync.iam.infrastructure.PatientProfileRepository;
import com.vitsync.iam.infrastructure.UserRepository;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Caso de uso: alta de un paciente (VIT-61).
 *
 * <p>Orden de los pasos, y por qué:
 * <ol>
 *   <li><b>Fuera de la transacción</b>: la consulta a HaveIBeenPwned (red, hasta 2 s) y el
 *       hash BCrypt (~250 ms). Si se hicieran dentro, cada registro tendría una conexión de
 *       la BD ocupada sin hacer nada mientras tanto.
 *   <li><b>Dentro de una transacción</b>: comprobar duplicados, crear {@code User} y
 *       {@code PatientProfile}, y encolar el email de verificación (outbox). Los dos o ninguno: así no puede quedar un paciente sin perfil,
 *       el fallo de v1 que el ADR-0003 quiere evitar.
 * </ol>
 *
 * <p>Se usa {@link TransactionTemplate} (transacción programática) en vez de
 * {@code @Transactional} porque solo una parte del método debe ser transaccional, y un
 * {@code @Transactional} en un método privado de la misma clase no tendría efecto (el proxy
 * de Spring solo intercepta llamadas que vienen de fuera).
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository users;
    private final PatientProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final BreachedPasswordChecker breachedPasswords;
    private final BlindIndex blindIndex;
    private final EmailVerificationService verification;
    private final TransactionTemplate tx;

    public RegistrationService(UserRepository users, PatientProfileRepository profiles,
                               PasswordEncoder passwordEncoder, BreachedPasswordChecker breachedPasswords,
                               BlindIndex blindIndex, EmailVerificationService verification,
                               TransactionTemplate tx) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.breachedPasswords = breachedPasswords;
        this.blindIndex = blindIndex;
        this.verification = verification;
        this.tx = tx;
    }

    public RegisteredPatient register(RegisterPatientCommand cmd) {
        // El DTO ya validó formato y letra de control; aquí se trabaja con la forma canónica.
        String email = Emails.normalize(cmd.email());
        String nif = Nif.normalize(cmd.nif());
        String nifHash = blindIndex.hash(nif);

        if (breachedPasswords.isBreached(cmd.password())) {
            throw new BusinessException(ErrorCode.PASSWORD_BREACHED);
        }
        String passwordHash = passwordEncoder.encode(cmd.password());

        try {
            User user = tx.execute(status -> {
                // Comprobación previa: el caso normal de duplicado responde sin llegar a
                // provocar un error de la BD.
                if (users.existsByEmailOrNifHash(email, nifHash)) {
                    throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
                }
                User created = users.saveAndFlush(User.newPatient(email, passwordHash,
                        cmd.firstName().strip(), cmd.lastName().strip(), nif, nifHash,
                        blankToNull(cmd.phone()), cmd.birthDate()));
                profiles.save(PatientProfile.emptyFor(created));
                // Código + email en el outbox, en esta misma transacción: si el alta se
                // deshace, no sale ningún email; si se confirma, el email saldrá seguro.
                verification.sendCode(created);
                return created;
            });
            // Solo el publicId: ni email ni NIF en los logs.
            log.info("Patient registered: {}", user.getPublicId());
            return new RegisteredPatient(user.getPublicId(), user.getEmail());
        } catch (DataIntegrityViolationException e) {
            if (!isUniqueViolation(e)) {
                throw e;   // otra restricción rota es un bug, no un duplicado: que salga como 500
            }
            // Dos registros simultáneos con el mismo email o NIF pasan los dos la comprobación
            // previa; el índice único de la BD frena al segundo. Es la garantía real: la
            // comprobación de arriba solo evita el error en el caso habitual.
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_EXISTS, e);
        }
    }

    // 23505 = unique_violation en PostgreSQL.
    private static boolean isUniqueViolation(DataIntegrityViolationException e) {
        return e.getMostSpecificCause() instanceof SQLException sql
                && "23505".equals(sql.getSQLState());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
