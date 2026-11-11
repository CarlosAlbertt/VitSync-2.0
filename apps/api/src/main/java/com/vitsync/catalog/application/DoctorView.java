package com.vitsync.catalog.application;

import java.util.List;
import java.util.UUID;

/**
 * Ficha pública de un médico del cuadro médico: datos de {@code iam} (nombre, colegiado,
 * biografía) y de {@code catalog} (especialidad y centros) juntados aquí (ADR-0009).
 */
public record DoctorView(UUID publicId, String firstName, String lastName, String licenseNumber, String bio,
                         SpecialtyRef specialty, List<FacilityRef> facilities) {

    public record SpecialtyRef(UUID publicId, String slug, String name) {
    }

    public record FacilityRef(UUID publicId, String name, String city) {
    }
}
