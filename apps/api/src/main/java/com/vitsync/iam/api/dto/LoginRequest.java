package com.vitsync.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/v1/auth/login}.
 *
 * <p>Solo se exige que vengan los dos campos: validar aquí el formato del email o la política
 * de contraseñas daría pistas distintas según el caso, y el login tiene que responder igual.
 */
public record LoginRequest(
        @NotBlank @Size(max = 254) @Schema(example = "paciente@vitsync.test") String email,
        @NotBlank @Size(max = 128) String password) {

    @Override
    public String toString() {
        return "LoginRequest[...]";
    }
}
