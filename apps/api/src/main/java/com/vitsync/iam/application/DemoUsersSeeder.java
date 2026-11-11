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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Usuarios de demostración, <b>solo en el perfil {@code demo}</b> (VIT-67 y VIT-81), que usa
 * su propia base de datos, {@code vitsync_dev} ({@code application-demo.properties}).
 *
 * <p>Por qué un {@link ApplicationRunner} y no una migración: los datos de demostración nunca
 * van en {@code db/migration} (CONTRIBUTING §6). Una migración se aplicaría también en
 * producción; esto, con {@code @Profile("demo")}, ni siquiera se crea como bean fuera de demo.
 *
 * <p>Idempotente: si el email ya existe, no toca nada. Datos sintéticos (NIF de ejemplo con
 * letra válida). La contraseña es pública a propósito y está en CONTRIBUTING; el paciente
 * coincide con el que usa el mock del frontend.
 *
 * <p>Además del médico de demostración, crea el cuadro médico que muestra el frontend
 * simulado (VIT-81), con los mismos nombres. La especialidad y los centros de cada uno los
 * asigna {@code catalog} después ({@code @Order}): son datos suyos y {@code iam} no depende de
 * {@code catalog} (ADR-0009). El enlace entre los dos seeders es el número de colegiado.
 */
@Component
@Profile("demo")
@Order(1)
class DemoUsersSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUsersSeeder.class);
    // Pública a propósito (CONTRIBUTING y .gitleaks.toml): solo existe en el perfil demo.
    static final String DEMO_PASSWORD = "Demostracion-2026";
    private static final String NIF_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    /** Médico del cuadro médico de demostración. El colegiado es la clave que usa catalog. */
    record DemoDoctor(String email, String firstName, String lastName, String licenseNumber, String bio) {
    }

    static final List<DemoDoctor> DEMO_DOCTORS = List.of(
            new DemoDoctor("ines.soler@vitsync.test", "Inés", "Soler Martí", "46/10000",
                    "Acompaña a familias enteras desde hace quince años. Cree en explicar las cosas sin prisa."),
            new DemoDoctor("tomas.aguirre@vitsync.test", "Tomás", "Aguirre Vidal", "46/10137",
                    "Especial interés en enfermedades crónicas y prevención."),
            new DemoDoctor("marta.bellver@vitsync.test", "Marta", "Bellver Ros", "46/10274",
                    "Cardiología preventiva y rehabilitación tras un evento cardíaco."),
            new DemoDoctor("hugo.requena@vitsync.test", "Hugo", "Requena Sanz", "46/10411",
                    "Ecocardiografía e insuficiencia cardíaca."),
            new DemoDoctor("clara.montoro@vitsync.test", "Clara", "Montoro Gil", "46/10548",
                    "Dermatoscopia digital y seguimiento de lunares."),
            new DemoDoctor("pau.esteve@vitsync.test", "Pau", "Esteve Roig", "46/10685",
                    "Consulta del niño sano y asma infantil."),
            new DemoDoctor("nuria.cano@vitsync.test", "Nuria", "Cano Ibáñez", "46/10822",
                    "Lactancia, alimentación y desarrollo en los primeros años."),
            new DemoDoctor("elena.vidal@vitsync.test", "Elena", "Vidal Ortuño", "46/10959",
                    "Seguimiento del embarazo y salud pélvica."),
            new DemoDoctor("alvaro.pastor@vitsync.test", "Álvaro", "Pastor Llorens", "46/11096",
                    "Rodilla, hombro y lesiones del deporte."),
            new DemoDoctor("sara.quiles@vitsync.test", "Sara", "Quiles Domingo", "46/11233",
                    "Retina y cirugía de cataratas."),
            new DemoDoctor("diego.marin@vitsync.test", "Diego", "Marín Tena", "46/11370",
                    "Cefaleas y trastornos del sueño."),
            new DemoDoctor("laia.ferrando@vitsync.test", "Laia", "Ferrando Pons", "46/11507",
                    "Ansiedad, depresión y terapia combinada."),
            new DemoDoctor("ruben.sancho@vitsync.test", "Rubén", "Sancho Beltrán", "46/11644",
                    "Resonancia musculoesquelética."),
            new DemoDoctor("carmen.ibars@vitsync.test", "Carmen", "Ibars Lluch", "46/11781",
                    "Hematología y bioquímica clínica."),
            new DemoDoctor("oscar.navarro@vitsync.test", "Óscar", "Navarro Mir", "46/11918",
                    "Diabetes tipo 1 y tecnología para el control de la glucosa."));

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
            seed(Role.ADMIN, "admin@vitsync.test", "Ada", "Administradora", "00000000T", null, null);
            seed(Role.DOCTOR, "medico@vitsync.test", "Mario", "Médico Demo", "00000001R", "DEMO-00001", null);
            seed(Role.PATIENT, "paciente@vitsync.test", "Paula", "Paciente Demo", "00000002W", null, null);
            for (int i = 0; i < DEMO_DOCTORS.size(); i++) {
                DemoDoctor d = DEMO_DOCTORS.get(i);
                seed(Role.DOCTOR, d.email(), d.firstName(), d.lastName(), nif(100 + i), d.licenseNumber(), d.bio());
            }
        });
    }

    /** NIF sintético con su letra de control correcta: 00000100 + letra, 00000101 + letra... */
    private static String nif(int number) {
        return "%08d%c".formatted(number, NIF_LETTERS.charAt(number % 23));
    }

    private void seed(Role role, String email, String firstName, String lastName, String nif,
                      String licenseNumber, String bio) {
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
            case DOCTOR -> {
                DoctorProfile profile = DoctorProfile.of(user, licenseNumber);
                profile.updateBio(bio);
                doctors.save(profile);
            }
            case ADMIN -> { }
        }
        log.info("Demo user created: {} ({})", email, role);
    }
}
