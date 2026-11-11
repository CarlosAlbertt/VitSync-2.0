package com.vitsync.catalog.application;

import com.vitsync.catalog.application.DoctorView.FacilityRef;
import com.vitsync.catalog.application.DoctorView.SpecialtyRef;
import com.vitsync.catalog.domain.Facility;
import com.vitsync.catalog.domain.Specialty;
import com.vitsync.catalog.infrastructure.FacilityRepository;
import com.vitsync.catalog.infrastructure.SpecialtyRepository;
import com.vitsync.iam.application.DoctorDirectory;
import com.vitsync.iam.application.ListedDoctor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuadro médico público (VIT-78): {@code catalog} es dueño de la consulta, y los datos del
 * médico los pide a {@code iam} (ADR-0009).
 *
 * <p>El número de consultas no depende del tamaño de la página:
 * <ol>
 *   <li>resolver los filtros que son de {@code catalog} (slug de especialidad, centro y sus
 *       médicos);</li>
 *   <li>{@code iam} devuelve la página de médicos ya filtrada y ordenada, con su {@code count};</li>
 *   <li>especialidades y centros de esa página, una consulta para cada cosa.</li>
 * </ol>
 */
@Service
public class DoctorCatalog {

    private final DoctorDirectory doctors;
    private final SpecialtyRepository specialties;
    private final FacilityRepository facilities;

    public DoctorCatalog(DoctorDirectory doctors, SpecialtyRepository specialties, FacilityRepository facilities) {
        this.doctors = doctors;
        this.specialties = specialties;
        this.facilities = facilities;
    }

    /**
     * @param specialtySlug    slug de una especialidad activa; {@code null} para todas
     * @param facilityPublicId centro activo; {@code null} para todos
     */
    @Transactional(readOnly = true)
    public Page<DoctorView> search(String specialtySlug, UUID facilityPublicId, int page, int size) {
        // Un filtro que no corresponde a nada (slug desconocido, centro cerrado) no es un error
        // del cliente: simplemente no hay médicos que lo cumplan.
        Long specialtyId = null;
        if (specialtySlug != null && !specialtySlug.isBlank()) {
            Optional<Specialty> specialty = specialties.findBySlugAndActiveTrue(specialtySlug.strip());
            if (specialty.isEmpty()) {
                return Page.empty(PageRequest.of(page, size));
            }
            specialtyId = specialty.get().getId();
        }
        Set<Long> atFacility = null;
        if (facilityPublicId != null) {
            Optional<Facility> facility = facilities.findByPublicIdAndActiveTrue(facilityPublicId);
            if (facility.isEmpty()) {
                return Page.empty(PageRequest.of(page, size));
            }
            atFacility = Set.copyOf(facilities.findDoctorIds(facility.get().getId()));
        }

        Page<ListedDoctor> result = doctors.listed(specialtyId, atFacility, page, size);
        if (result.isEmpty()) {
            return result.map(d -> null); // página vacía con sus metadatos; no hay nada que juntar
        }

        Set<Long> doctorIds = result.stream().map(ListedDoctor::id).collect(Collectors.toSet());
        Map<Long, Specialty> specialtyById = specialties.findAllById(
                        result.stream().map(ListedDoctor::specialtyId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Specialty::getId, Function.identity()));
        Map<Long, List<FacilityRef>> facilitiesByDoctor = facilities.findActiveByDoctorIds(doctorIds).stream()
                .collect(Collectors.groupingBy(FacilityRepository.DoctorFacilityRow::getDoctorId,
                        Collectors.mapping(r -> new FacilityRef(r.getPublicId(), r.getName(), r.getCity()),
                                Collectors.toList())));

        return result.map(d -> {
            Specialty s = specialtyById.get(d.specialtyId());
            return new DoctorView(d.publicId(), d.firstName(), d.lastName(), d.licenseNumber(), d.bio(),
                    new SpecialtyRef(s.getPublicId(), s.getSlug(), s.getName()),
                    facilitiesByDoctor.getOrDefault(d.id(), List.of()));
        });
    }
}
