package com.vitsync.iam.api.dto;

import com.vitsync.iam.api.validation.ValidNif;
import com.vitsync.iam.api.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cuerpo de {@code POST /api/v1/auth/register}.
 *
 * <p>Las anotaciones son a la vez validación (400 con la lista de campos) y documentación:
 * springdoc las lee y las publica en el contrato OpenAPI.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254)
        @Schema(example = "paciente@vitsync.test")
        String email,

        @NotBlank @ValidPassword
        @Schema(minLength = 12, description = "12+ characters, at most 72 bytes in UTF-8")
        String password,

        @NotBlank @Size(max = 100)
        @Schema(example = "Lucía")
        String firstName,

        @NotBlank @Size(max = 100)
        @Schema(example = "García Pérez")
        String lastName,

        @NotBlank @ValidNif
        @Schema(description = "Spanish NIF (DNI) or NIE with a valid control letter", example = "12345678Z")
        String nif,

        @NotNull @Past
        @Schema(example = "1990-05-17")
        LocalDate birthDate,

        @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "must be 9 to 15 digits, optionally starting with +")
        @Schema(example = "+34600000000")
        String phone) {

    @Override
    public String toString() {
        return "RegisterRequest[...]";
    }
}
