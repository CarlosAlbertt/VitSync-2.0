package com.vitsync.iam.application;

import com.vitsync.common.crypto.BlindIndex;
import com.vitsync.iam.domain.DoctorProfile;
import com.vitsync.iam.domain.PatientProfile;
import com.vitsync.iam.domain.Role;
import com.vitsync.iam.domain.User;
import com.vitsync.iam.infrastructure.DoctorProfileRepository;
import com.vitsync.iam.infrastructure.PatientProfileRepository;
import com.vitsync.iam.infrastructure.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Usuarios de demostración, <b>solo en el perfil {@code local}</b> (VIT-67, decisión 8 del
 * plan del Sprint 1).
 *
 * <p>Por qué un {@link ApplicationRunner} y no una migración: los datos de demostración nunca
 * van en {@code db/migration} (CONTRIBUTING §6). Una migración se aplicaría también en
 * producción; esto, con {@code @Profile("local")}, ni siquiera se crea como bean fuera de local.
 *
 * <p>Idempotente: si el email ya existe, no toca nada. Datos sintéticos (NIF de ejemplo con
 * letra válida). La contraseña es pública a propósito y está en CONTRIBUTING; el paciente
 * coincide con el que usa el mock del frontend.
 */
@Component
@Profile("local")
class DemoUsersSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUsersSeeder.class);
    // Pública a propósito (CONTRIBUTING y .gitleaks.toml): solo existe en el perfil local.
    static final String DEMO_PASSWORD = "Demostracion-2026";

    private final UserRepository users;
    private final PatientProfileRepository patients;
    private final DoctorProfileRepository doctors;
    private final PasswordEncoder passwordEncoder;
    private final BlindIndex blindIndex;
    private final TransactionTemplate tx;
    private final Clock clock;

    DemoUsersSeeder(UserRepository users, PatientProfileRepository patients, DoctorProfileRepository doctors,
                    PasswordEncoder passwordEncoder, BlindIndex blindIndex, TransactionTemplate tx, Clock clock) {
        this.users = users;
        this.patients = patients;
        this.doctors = doctors;
        this.passwordEncoder = passwordEncoder;
        this.blindIndex = blindIndex;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(s -> {
            seed(Role.ADMIN, "admin@vitsync.test", "Ada", "Administradora", "00000000T");
            seed(Role.DOCTOR, "medico@vitsync.test", "Mario", "Médico Demo", "00000001R");
            seed(Role.PATIENT, "paciente@vitsync.test", "Paula", "Paciente Demo", "00000002W");
        });
    }

    private void seed(Role role, String email, String firstName, String lastName, String nif) {
        if (users.existsByEmail(email)) {
            return;
        }
        User user = User.newAccount(role, email, passwordEncoder.encode(DEMO_PASSWORD), firstName, lastName,
                nif, blindIndex.hash(nif), null, LocalDate.of(1990, 1, 1));
        user.activate(clock.instant());
        users.saveAndFlush(user);
        // Cada rol con su perfil, en la misma transacción (ADR-0003).
        switch (role) {
            case PATIENT -> patients.save(PatientProfile.emptyFor(user));
            case DOCTOR -> doctors.save(DoctorProfile.of(user, "DEMO-00001"));
            case ADMIN -> { }
        }
        log.info("Demo user created: {} ({})", email, role);
    }
}
