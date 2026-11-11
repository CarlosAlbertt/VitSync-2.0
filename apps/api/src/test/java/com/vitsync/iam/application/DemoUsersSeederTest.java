package com.vitsync.iam.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitsync.support.IntegrationTest;
import com.vitsync.support.TestAccounts;
import com.vitsync.support.TestAccounts.Account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Usuarios demo con el perfil {@code demo} (VIT-67).
 *
 * <p>El worker del outbox se apaga a mano: el perfil local lo enciende, y un worker vivo en
 * este contexto cogería los mensajes de otros tests (la BD es común) y los haría fallar al no
 * haber Mailpit.
 */
// local + demo, como se arranca en desarrollo. La URL de vitsync_dev del perfil demo no se
// usa: @ServiceConnection la sustituye por la del contenedor de Testcontainers.
@ActiveProfiles({"local", "demo"})
@TestPropertySource(properties = "vitsync.outbox.worker.enabled=false")
class DemoUsersSeederTest extends IntegrationTest {

    @Autowired DemoUsersSeeder seeder;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    void local_profile_creates_three_active_demo_users_with_their_profiles() {
        assertThat(jdbc.queryForList("SELECT u.role FROM users u WHERE u.email IN "
                + "('admin@vitsync.test','medico@vitsync.test','paciente@vitsync.test') AND u.status = 'ACTIVE' "
                + "ORDER BY u.role", String.class)).containsExactly("ADMIN", "DOCTOR", "PATIENT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM patient_profiles p JOIN users u ON u.id = p.user_id "
                + "WHERE u.email = 'paciente@vitsync.test'", Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM doctor_profiles d JOIN users u ON u.id = d.user_id "
                + "WHERE u.email = 'medico@vitsync.test'", Integer.class)).isOne();
    }

    @Test
    void seeding_twice_changes_nothing() {
        int before = jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);
        seeder.run(new DefaultApplicationArguments());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(before);
    }

    @Test
    void demo_patient_logs_in_with_the_documented_password() throws Exception {
        // La misma pareja que usa el mock del frontend.
        TestAccounts.LoggedIn session = TestAccounts.login(mvc,
                new Account("paciente@vitsync.test", DemoUsersSeeder.DEMO_PASSWORD));
        assertThat(session.body().get("user").get("role").asString()).isEqualTo("PATIENT");
    }
}
