package com.vitsync.catalog.application;

import com.vitsync.catalog.domain.Specialty;
import com.vitsync.catalog.domain.SpecialtyKind;
import com.vitsync.catalog.infrastructure.SpecialtyRepository;
import com.vitsync.iam.application.DoctorDirectory;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta pública de especialidades (VIT-76). */
@Service
public class SpecialtyCatalog {

    private final SpecialtyRepository specialties;
    private final DoctorDirectory doctors;

    public SpecialtyCatalog(SpecialtyRepository specialties, DoctorDirectory doctors) {
        this.specialties = specialties;
        this.doctors = doctors;
    }

    /**
     * Página de especialidades activas, por nombre.
     *
     * <p>El número de médicos lo sabe {@code iam}: se le pide para toda la página de una vez
     * (ADR-0009), así son dos consultas por página y no una por especialidad.
     *
     * @param text texto libre sobre el nombre, con o sin tildes; {@code null} para no filtrar
     * @param kind tipo de especialidad; {@code null} para todos
     */
    @Transactional(readOnly = true)
    public Page<SpecialtyView> search(String text, SpecialtyKind kind, int page, int size) {
        Set<SpecialtyKind> kinds = kind == null ? EnumSet.allOf(SpecialtyKind.class) : EnumSet.of(kind);
        Page<Specialty> result = specialties.searchActive(kinds,
                SearchPatterns.contains(text), SearchPatterns.containsAsSlug(text),
                PageRequest.of(page, size, Sort.by("name", "id")));

        Map<Long, Long> doctorCounts = doctors.countListedBySpecialty(
                result.getContent().stream().map(Specialty::getId).collect(Collectors.toSet()));
        return result.map(s -> new SpecialtyView(s.getPublicId(), s.getSlug(), s.getName(), s.getDescription(),
                s.getKind().name(), doctorCounts.getOrDefault(s.getId(), 0L)));
    }
}
