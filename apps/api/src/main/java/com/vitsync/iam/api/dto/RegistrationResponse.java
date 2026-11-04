package com.vitsync.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del registro. El email sirve al frontend para decir "te hemos enviado un código
 * a ...". No hay token: la cuenta no puede iniciar sesión hasta verificar el email.
 */
public record RegistrationResponse(
        @Schema(example = "paciente@vitsync.test") String email,
        @Schema(example = "PENDING_VERIFICATION") String status) {
}
