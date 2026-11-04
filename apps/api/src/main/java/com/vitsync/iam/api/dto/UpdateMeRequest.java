package com.vitsync.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PATCH /api/v1/me}. Semántica de PATCH: un campo ausente ({@code null}) no
 * se toca. {@code phone: ""} borra el teléfono. Email, NIF y rol no se cambian por aquí.
 */
public record UpdateMeRequest(
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String firstName,
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String lastName,
        @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "must be 9 to 15 digits, optionally starting with +")
        @Schema(description = "Empty string removes the phone") String phone,
        @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "must look like es-ES")
        @Schema(example = "es-ES") String locale) {

    @Override
    public String toString() {
        return "UpdateMeRequest[...]";
    }
}
