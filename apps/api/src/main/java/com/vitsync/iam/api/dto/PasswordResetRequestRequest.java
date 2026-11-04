package com.vitsync.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/v1/auth/password-reset/request}. Sin {@code @Email}: un email mal
 * formado recibe la misma respuesta que uno que no existe.
 */
public record PasswordResetRequestRequest(
        @NotBlank @Size(max = 254) @Schema(example = "paciente@vitsync.test") String email) {

    @Override
    public String toString() {
        return "PasswordResetRequestRequest[...]";
    }
}
