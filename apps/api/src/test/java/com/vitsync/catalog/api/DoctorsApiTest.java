package com.vitsync.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.catalog.domain.Facility;
import com.vitsync.catalog.domain.Specialty;
import com.vitsync.catalog.domain.SpecialtyKind;
import com.vitsync.catalog.infrastructure.FacilityRepository;
import com.vitsync.catalog.infrastructure.SpecialtyRepository;
import com.vitsync.support.IntegrationTest;
import com.vitsync.support.TestAccounts;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code GET /api/v1/doctors} de punta a punta (VIT-78).
 *
 * <p>Cada test crea su propia especialidad y sus centros, y filtra por ellos: la base de datos
 * es común a toda la suite y puede tener médicos de otros tests o del perfil demo.
 */
class DoctorsApiTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired SpecialtyRepository specialties;
    @Autowired FacilityRepository facilities;
    @Autowired EntityManagerFactory emf;

    private String token;
    private Specialty specialty;
    private Facility north;
    private Facility south;

    @BeforeEach
    void catalog() {
        token = "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        specialty = specialties.save(Specialty.create("S_" + token.toUpperCase(), "esp-" + token,
                "Especialidad " + token, null, SpecialtyKind.MEDICAL));
        north = facilities.save(Facility.create("Norte " + token, "Calle 1", "València", "46001", null));
        south = facilities.save(Facility.create("Sur " + token, "Calle 2", "Madrid", "28001", null));
    }

    @Test
    void filter_by_specialty_returns_only_public_data_sorted_by_last_name() throws Exception {
        doctor("Zoe", "Zamora", specialty, List.of(north, south));
        doctor("Ana", "Abad", specialty, List.of(north));

        mvc.perform(get("/api/v1/doctors").param("specialty", specialty.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].lastName").value("Abad"))
                .andExpect(jsonPath("$.content[0].firstName").value("Ana"))
                .andExpect(jsonPath("$.content[0].publicId").isNotEmpty())
                .andExpect(jsonPath("$.content[0].licenseNumber").isNotEmpty())
                .andExpect(jsonPath("$.content[0].bio").value("Bio de Ana"))
                .andExpect(jsonPath("$.content[0].specialty.slug").value(specialty.getSlug()))
                .andExpect(jsonPath("$.content[0].specialty.name").value(specialty.getName()))
                .andExpect(jsonPath("$.content[0].facilities.length()").value(1))
                .andExpect(jsonPath("$.content[0].facilities[0].publicId").value(north.getPublicId().toString()))
                .andExpect(jsonPath("$.content[0].facilities[0].city").value("València"))
                .andExpect(jsonPath("$.content[1].lastName").value("Zamora"))
                .andExpect(jsonPath("$.content[1].facilities.length()").value(2))
                // Datos que nunca salen en el cuadro médico.
                .andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].phone").doesNotExist())
                .andExpect(jsonPath("$.content[0].nif").doesNotExist());
    }

    @Test
    void filter_by_facility_and_by_both() throws Exception {
        doctor("Ana", "Abad", specialty, List.of(north));
        doctor("Bea", "Bravo", specialty, List.of(south));

        mvc.perform(get("/api/v1/doctors").param("facility", south.getPublicId().toString()))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Bravo"));
        mvc.perform(get("/api/v1/doctors").param("specialty", specialty.getSlug())
                        .param("facility", north.getPublicId().toString()))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Abad"));
    }

    @Test
    void filtering_happens_before_paging() throws Exception {
        // Si se filtrara en memoria después de paginar, el total no sería el de los filtrados.
        doctor("Ana", "Abad", specialty, List.of(north));
        doctor("Bea", "Bravo", specialty, List.of(north));
        doctor("Ciro", "Cano", specialty, List.of(north));

        mvc.perform(get("/api/v1/doctors").param("specialty", specialty.getSlug()).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Cano"))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void doctors_not_open_to_the_public_are_left_out() throws Exception {
        doctor("Ana", "Abad", specialty, List.of(north));
        Long retired = doctor("Bea", "Bravo", specialty, List.of(north));
        jdbc.update("UPDATE doctor_profiles SET active = false WHERE user_id = ?", retired);
        Long suspended = doctor("Ciro", "Cano", specialty, List.of(north));
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", suspended);
        Long deleted = doctor("Dani", "Díaz", specialty, List.of(north));
        jdbc.update("UPDATE users SET deleted_at = now() WHERE id = ?", deleted);
        Long unassigned = doctor("Eva", "Esteve", specialty, List.of(north));
        jdbc.update("UPDATE doctor_profiles SET specialty_id = NULL WHERE user_id = ?", unassigned);

        mvc.perform(get("/api/v1/doctors").param("facility", north.getPublicId().toString()))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Abad"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void closed_facilities_are_not_shown_on_the_doctor() throws Exception {
        doctor("Ana", "Abad", specialty, List.of(north, south));
        jdbc.update("UPDATE facilities SET active = false WHERE id = ?", south.getId());

        mvc.perform(get("/api/v1/doctors").param("specialty", specialty.getSlug()))
                .andExpect(jsonPath("$.content[0].facilities.length()").value(1))
                .andExpect(jsonPath("$.content[0].facilities[0].name").value(north.getName()));
    }

    @Test
    void the_number_of_queries_does_not_grow_with_the_page() throws Exception {
        // Sin N+1: una página de 2 médicos y otra de 6 hacen las mismas consultas.
        doctor("Ana", "Abad", specialty, List.of(north, south));
        doctor("Bea", "Bravo", specialty, List.of(north));
        long withTwo = queriesFor(specialty.getSlug());

        Specialty other = specialties.save(Specialty.create("O_" + token.toUpperCase(), "otra-" + token,
                "Otra " + token, null, SpecialtyKind.MEDICAL));
        for (String name : List.of("Ciro", "Dani", "Eva", "Fran", "Gala", "Hugo")) {
            doctor(name, name + "ez", other, List.of(north, south));
        }
        long withSix = queriesFor(other.getSlug());

        assertThat(withSix).isEqualTo(withTwo);
    }

    private long queriesFor(String slug) throws Exception {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        mvc.perform(get("/api/v1/doctors").param("specialty", slug)).andExpect(status().isOk());
        return stats.getPrepareStatementCount();
    }

    @Test
    void unknown_specialty_or_facility_gives_an_empty_page() throws Exception {
        mvc.perform(get("/api/v1/doctors").param("specialty", "no-existe-" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/v1/doctors").param("facility", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void facility_without_doctors_gives_an_empty_page() throws Exception {
        mvc.perform(get("/api/v1/doctors").param("facility", north.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void malformed_facility_id_is_a_bad_request() throws Exception {
        mvc.perform(get("/api/v1/doctors").param("facility", "fac-turia"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void oversized_page_is_rejected() throws Exception {
        mvc.perform(get("/api/v1/doctors").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    /**
     * Médico con cuenta activa: se registra por la API, se promueve por SQL y se le asignan
     * especialidad y centros. Devuelve su id interno.
     */
    private Long doctor(String firstName, String lastName, Specialty spec, List<Facility> at) throws Exception {
        String email = TestAccounts.active(mvc, jdbc).email();
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        jdbc.update("UPDATE users SET role = 'DOCTOR', first_name = ?, last_name = ? WHERE id = ?",
                firstName, lastName, id);
        jdbc.update("INSERT INTO doctor_profiles (user_id, license_number, specialty_id, bio) VALUES (?, ?, ?, ?)",
                id, "T-" + id, spec.getId(), "Bio de " + firstName);
        at.forEach(f -> jdbc.update("INSERT INTO doctor_facilities (doctor_id, facility_id) VALUES (?, ?)",
                id, f.getId()));
        return id;
    }
}
