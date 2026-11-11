package com.vitsync.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.vitsync.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Catálogo de demostración con el perfil {@code demo} (VIT-81). Misma configuración que
 * {@code DemoUsersSeederTest}, así los dos comparten contexto de Spring.
 */
// local + demo, como se arranca en desarrollo. La URL de vitsync_dev del perfil demo no se
// usa: @ServiceConnection la sustituye por la del contenedor de Testcontainers.
@ActiveProfiles({"local", "demo"})
@TestPropertySource(properties = "vitsync.outbox.worker.enabled=false")
class CatalogDemoSeederTest extends IntegrationTest {

    @Autowired CatalogDemoSeeder seeder;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    void local_profile_creates_the_demo_specialties_and_facilities() {
        assertThat(jdbc.queryForList("SELECT code FROM specialties WHERE active", String.class))
                .containsAll(CatalogDemoSeeder.SPECIALTIES.stream().map(CatalogDemoSeeder.DemoSpecialty::code).toList());
        assertThat(jdbc.queryForList("SELECT name FROM facilities WHERE active", String.class))
                .containsAll(CatalogDemoSeeder.FACILITIES.stream().map(CatalogDemoSeeder.DemoFacility::name).toList());
    }

    @Test
    void every_demo_doctor_has_its_specialty_and_facilities() {
        // Incluye al médico demo del Sprint 1 (DEMO-00001), que es el criterio de VIT-81.
        for (CatalogDemoSeeder.Assignment a : CatalogDemoSeeder.ASSIGNMENTS) {
            assertThat(jdbc.queryForObject("""
                    SELECT s.code FROM doctor_profiles d JOIN specialties s ON s.id = d.specialty_id
                    WHERE d.license_number = ?""", String.class, a.licenseNumber()))
                    .as("specialty of %s", a.licenseNumber()).isEqualTo(a.specialtyCode());
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM doctor_facilities df JOIN doctor_profiles d ON d.user_id = df.doctor_id
                    WHERE d.license_number = ?""", Integer.class, a.licenseNumber()))
                    .as("facilities of %s", a.licenseNumber()).isEqualTo(a.facilityNames().size());
        }
    }

    @Test
    void the_public_api_counts_the_demo_doctors() throws Exception {
        mvc.perform(get("/api/v1/specialties").param("q", "cardiologia"))
                .andExpect(jsonPath("$.content[?(@.slug == 'cardiologia')].doctorCount").value(2));
    }

    @Test
    void the_demo_doctors_appear_in_the_public_directory() throws Exception {
        mvc.perform(get("/api/v1/doctors").param("specialty", "cardiologia"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].lastName").value("Bellver Ros"))
                .andExpect(jsonPath("$.content[0].facilities[0].name").value("Centro Jardín del Turia"))
                .andExpect(jsonPath("$.content[1].lastName").value("Requena Sanz"));
    }

    @Test
    void seeding_twice_changes_nothing() {
        int specialties = jdbc.queryForObject("SELECT count(*) FROM specialties", Integer.class);
        int facilities = jdbc.queryForObject("SELECT count(*) FROM facilities", Integer.class);
        int links = jdbc.queryForObject("SELECT count(*) FROM doctor_facilities", Integer.class);

        seeder.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM specialties", Integer.class)).isEqualTo(specialties);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM facilities", Integer.class)).isEqualTo(facilities);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM doctor_facilities", Integer.class)).isEqualTo(links);
    }
}
