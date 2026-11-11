package com.vitsync.catalog.application;

import com.vitsync.catalog.infrastructure.FacilityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta pública de centros (VIT-77). */
@Service
public class FacilityCatalog {

    private final FacilityRepository facilities;

    public FacilityCatalog(FacilityRepository facilities) {
        this.facilities = facilities;
    }

    /** Centros activos, agrupados por ciudad y después por nombre. */
    @Transactional(readOnly = true)
    public Page<FacilityView> list(int page, int size) {
        return facilities.findByActiveTrue(PageRequest.of(page, size, Sort.by("city", "name", "id")))
                .map(f -> new FacilityView(f.getPublicId(), f.getName(), f.getAddress(), f.getCity(),
                        f.getPostalCode(), f.getPhone()));
    }
}
