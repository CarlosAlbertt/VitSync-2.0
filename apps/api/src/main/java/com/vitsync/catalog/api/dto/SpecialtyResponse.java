package com.vitsync.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Especialidad del catálogo público ({@code Specialty} en el frontend). */
public record SpecialtyResponse(
        UUID publicId,
        @Schema(example = "cardiologia") String slug,
        @Schema(example = "Cardiología") String name,
        @Schema(nullable = true, example = "Corazón y sistema circulatorio, de la prevención al seguimiento.")
        String description,
        @Schema(allowableValues = {"MEDICAL", "SURGICAL", "DIAGNOSTIC", "GENERAL", "UNIT"}) String kind,
        @Schema(description = "Doctors currently listed in this specialty", example = "2") long doctorCount) {
}
