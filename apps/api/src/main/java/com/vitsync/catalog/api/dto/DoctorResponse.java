package com.vitsync.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Ficha pública de un médico ({@code Doctor} en el frontend). Solo datos públicos: ni email,
 * ni teléfono, ni NIF. {@code publicId} es el id del médico en toda la API.
 */
public record DoctorResponse(
        UUID publicId,
        @Schema(example = "Marta") String firstName,
        @Schema(example = "Bellver Ros") String lastName,
        @Schema(description = "Medical license number (public)", example = "46/10274") String licenseNumber,
        @Schema(nullable = true, example = "Cardiología preventiva y rehabilitación tras un evento cardíaco.")
        String bio,
        SpecialtySummary specialty,
        @Schema(description = "Active facilities where the doctor sees patients") List<FacilitySummary> facilities) {

    public record SpecialtySummary(
            UUID publicId,
            @Schema(example = "cardiologia") String slug,
            @Schema(example = "Cardiología") String name) {
    }

    public record FacilitySummary(
            UUID publicId,
            @Schema(example = "Centro Jardín del Turia") String name,
            @Schema(example = "València") String city) {
    }
}
