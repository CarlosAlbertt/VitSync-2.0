package com.vitsync.iam.application;

import com.vitsync.iam.domain.DoctorProfile;
import java.util.UUID;

/**
 * Datos públicos de un médico, para otros módulos (ADR-0009). Solo lo que puede ver cualquier
 * visitante: sin email, teléfono, NIF ni fecha de nacimiento. El número de colegiado es
 * público por ley (registro de colegiados).
 *
 * @param id          id interno, para que el módulo que llama cruce con sus datos; nunca sale
 *                    por la API
 * @param publicId    el {@code public_id} del usuario: el id del médico hacia fuera
 * @param specialtyId id de la especialidad en {@code catalog}
 */
public record ListedDoctor(Long id, UUID publicId, String firstName, String lastName, String licenseNumber,
                           String bio, Long specialtyId) {

    static ListedDoctor of(DoctorProfile d) {
        return new ListedDoctor(d.getUserId(), d.getUser().getPublicId(), d.getUser().getFirstName(),
                d.getUser().getLastName(), d.getLicenseNumber(), d.getBio(), d.getSpecialtyId());
    }
}
