package com.vitsync.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de {@code POST /api/v1/auth/verify}. */
public record VerifyEmailRequest(
        @NotBlank @Email @Schema(example = "paciente@vitsync.test") String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be 6 digits")
        @Schema(example = "123456") String code) {

    @Override
    public String toString() {
        return "VerifyEmailRequest[...]";
    }
}
