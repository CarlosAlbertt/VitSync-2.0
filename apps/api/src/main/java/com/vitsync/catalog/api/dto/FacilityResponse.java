package com.vitsync.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Centro médico del catálogo público ({@code Facility} en el frontend). */
public record FacilityResponse(
        UUID publicId,
        @Schema(example = "Centro Jardín del Turia") String name,
        @Schema(example = "Paseo de la Ribera 12") String address,
        @Schema(example = "València") String city,
        @Schema(example = "46010") String postalCode,
        @Schema(nullable = true, example = "+34 960 000 010") String phone) {
}
