package com.vitsync.iam.application;

import com.vitsync.common.error.BusinessException;
import com.vitsync.common.error.ErrorCode;
import com.vitsync.iam.domain.DoctorProfile;
import com.vitsync.iam.infrastructure.DoctorProfileRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puerta de {@code iam} para que otros módulos consulten y asignen datos de médicos sin tocar
 * sus entidades (ADR-0009). Todo lo que devuelve son ids o valores, nunca un
 * {@link DoctorProfile}: si lo hiciera, el módulo que llama dependería de {@code iam.domain} y
 * {@code ArchitectureTest} fallaría.
 *
 * <p>Los métodos de lectura trabajan por lote: reciben todos los ids de una página y resuelven
 * en una consulta.
 */
@Service
public class DoctorDirectory {

    private final DoctorProfileRepository doctors;

    public DoctorDirectory(DoctorProfileRepository doctors) {
        this.doctors = doctors;
    }

    /**
     * Médicos visibles por especialidad. Las especialidades sin médicos no aparecen en el
     * mapa: quien llama usa 0 por defecto.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> countListedBySpecialty(Collection<Long> specialtyIds) {
        if (specialtyIds.isEmpty()) {
            return Map.of();
        }
        return doctors.countListedBySpecialty(specialtyIds).stream()
                .collect(Collectors.toMap(DoctorProfileRepository.SpecialtyCount::getSpecialtyId,
                        DoctorProfileRepository.SpecialtyCount::getTotal));
    }

    /** Id interno de cada médico a partir de su número de colegiado, que es público y único. */
    @Transactional(readOnly = true)
    public Map<String, Long> idsByLicenseNumber(Collection<String> licenseNumbers) {
        return doctors.findByLicenseNumberIn(licenseNumbers).stream()
                .collect(Collectors.toMap(DoctorProfile::getLicenseNumber, DoctorProfile::getUserId));
    }

    @Transactional
    public void assignSpecialty(Long doctorId, Long specialtyId) {
        doctors.findById(doctorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))
                .assignSpecialty(specialtyId);
    }
}
