package com.vitsync.catalog.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitsync.catalog.domain.Facility;
import com.vitsync.catalog.infrastructure.FacilityRepository;
import com.vitsync.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** {@code GET /api/v1/facilities} de punta a punta (VIT-77). */
class FacilitiesApiTest extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired FacilityRepository facilities;

    @Test
    void anonymous_visitor_sees_active_facilities_with_their_contact_details() throws Exception {
        String name = "Centro " + UUID.randomUUID();
        Facility active = facilities.save(Facility.create(name, "Calle Mayor 1", "València", "46001", "+34 960 000 999"));
        Facility closed = Facility.create("Cerrado " + UUID.randomUUID(), "Calle Menor 2", "València", "46002", null);
        closed.deactivate();
        facilities.save(closed);

        String item = "$.content[?(@.publicId == '" + active.getPublicId() + "')]";
        mvc.perform(get("/api/v1/facilities").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(item + ".name").value(name))
                .andExpect(jsonPath(item + ".address").value("Calle Mayor 1"))
                .andExpect(jsonPath(item + ".city").value("València"))
                .andExpect(jsonPath(item + ".postalCode").value("46001"))
                .andExpect(jsonPath(item + ".phone").value("+34 960 000 999"))
                .andExpect(jsonPath("$.content[?(@.publicId == '" + closed.getPublicId() + "')]").isEmpty())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void negative_page_is_rejected() throws Exception {
        mvc.perform(get("/api/v1/facilities").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }
}
