package com.vitsync.iam.api.dto;

import com.vitsync.iam.api.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/auth/password-reset}. */
public record PasswordResetRequest(
        @NotBlank @Size(max = 254) @Schema(example = "paciente@vitsync.test") String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be 6 digits") @Schema(example = "123456") String code,
        @NotBlank @ValidPassword
        @Schema(minLength = 12, description = "12+ characters, at most 72 bytes in UTF-8") String newPassword) {

    @Override
    public String toString() {
        return "PasswordResetRequest[...]";
    }
}
