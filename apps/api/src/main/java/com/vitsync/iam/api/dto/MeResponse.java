package com.vitsync.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** El usuario autenticado ({@code Me} en el frontend). {@code id} es el {@code public_id}. */
public record MeResponse(
        UUID id,
        @Schema(example = "paciente@vitsync.test") String email,
        @Schema(allowableValues = {"PATIENT", "DOCTOR", "ADMIN"}) String role,
        @Schema(example = "Lucía") String firstName,
        @Schema(example = "García Pérez") String lastName,
        @Schema(nullable = true, example = "+34600000000") String phone,
        @Schema(example = "es-ES") String locale,
        boolean emailVerified) {
}
