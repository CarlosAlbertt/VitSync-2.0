package com.vitsync.catalog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.catalog.domain.Specialty;
import com.vitsync.catalog.domain.SpecialtyKind;
import com.vitsync.catalog.infrastructure.SpecialtyRepository;
import com.vitsync.support.IntegrationTest;
import com.vitsync.support.TestAccounts;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code GET /api/v1/specialties} de punta a punta (VIT-76).
 *
 * <p>La base de datos es común a toda la suite y el perfil local siembra su catálogo en ella,
 * así que cada test crea sus especialidades con un token aleatorio y filtra por él: nunca
 * cuenta con que el catálogo esté vacío.
 */
class SpecialtiesApiTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired SpecialtyRepository specialties;

    private String token;

    @BeforeEach
    void newToken() {
        token = "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private Specialty create(String name, SpecialtyKind kind, boolean active) {
        String slug = name.toLowerCase(Locale.ROOT).replace("í", "i").replace(" ", "-");
        Specialty s = Specialty.create(slug.toUpperCase(Locale.ROOT).replace("-", "_"), slug, name, "Descripción", kind);
        if (!active) {
            s.deactivate();
        }
        return specialties.save(s);
    }

    @Test
    void anonymous_visitor_gets_a_page_of_active_specialties_sorted_by_name() throws Exception {
        create("Zeta " + token, SpecialtyKind.MEDICAL, true);
        create("Alfa " + token, SpecialtyKind.SURGICAL, true);
        create("Retirada " + token, SpecialtyKind.MEDICAL, false);

        mvc.perform(get("/api/v1/specialties").param("q", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Alfa " + token))
                .andExpect(jsonPath("$.content[0].slug").value("alfa-" + token))
                .andExpect(jsonPath("$.content[0].kind").value("SURGICAL"))
                .andExpect(jsonPath("$.content[0].publicId").isNotEmpty())
                .andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content[1].name").value("Zeta " + token))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void pages_are_cut_by_size_and_numbered_from_zero() throws Exception {
        create("Alfa " + token, SpecialtyKind.MEDICAL, true);
        create("Beta " + token, SpecialtyKind.MEDICAL, true);
        create("Gamma " + token, SpecialtyKind.MEDICAL, true);

        mvc.perform(get("/api/v1/specialties").param("q", token).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Gamma " + token))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void search_matches_with_or_without_accents() throws Exception {
        create("Cardiología " + token, SpecialtyKind.MEDICAL, true);

        mvc.perform(get("/api/v1/specialties").param("q", "cardiologia " + token))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/v1/specialties").param("q", "CARDIOLOGÍA " + token))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void like_wildcards_typed_by_the_user_are_literal() throws Exception {
        create("Alfa " + token, SpecialtyKind.MEDICAL, true);

        mvc.perform(get("/api/v1/specialties").param("q", token + "%"))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void kind_filter_is_applied() throws Exception {
        create("Alfa " + token, SpecialtyKind.DIAGNOSTIC, true);
        create("Beta " + token, SpecialtyKind.MEDICAL, true);

        mvc.perform(get("/api/v1/specialties").param("q", token).param("kind", "DIAGNOSTIC"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alfa " + token));
    }

    @Test
    void doctor_count_includes_only_listed_doctors() throws Exception {
        Specialty specialty = create("Alfa " + token, SpecialtyKind.MEDICAL, true);
        addDoctor(specialty.getId(), true);
        addDoctor(specialty.getId(), true);
        addDoctor(specialty.getId(), false); // perfil retirado: no se cuenta

        mvc.perform(get("/api/v1/specialties").param("q", token))
                .andExpect(jsonPath("$.content[0].doctorCount").value(2));
    }

    @Test
    void specialty_without_doctors_counts_zero() throws Exception {
        create("Alfa " + token, SpecialtyKind.MEDICAL, true);

        mvc.perform(get("/api/v1/specialties").param("q", token))
                .andExpect(jsonPath("$.content[0].doctorCount").value(0));
    }

    @Test
    void oversized_page_is_rejected_with_the_failing_parameter() throws Exception {
        mvc.perform(get("/api/v1/specialties").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void unknown_kind_is_a_bad_request() throws Exception {
        mvc.perform(get("/api/v1/specialties").param("kind", "ASTROLOGY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void writing_to_the_catalog_stays_closed_to_anonymous_visitors() throws Exception {
        mvc.perform(post("/api/v1/specialties").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    /** Médico con cuenta activa: se registra por la API y se promueve por SQL. */
    private void addDoctor(Long specialtyId, boolean profileActive) throws Exception {
        String email = TestAccounts.active(mvc, jdbc).email();
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        jdbc.update("UPDATE users SET role = 'DOCTOR' WHERE id = ?", userId);
        jdbc.update("INSERT INTO doctor_profiles (user_id, license_number, specialty_id, active) VALUES (?, ?, ?, ?)",
                userId, "T-" + userId, specialtyId, profileActive);
    }
}
