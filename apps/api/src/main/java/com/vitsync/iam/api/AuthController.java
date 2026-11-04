package com.vitsync.iam.api;

import com.vitsync.iam.api.dto.RegisterRequest;
import com.vitsync.iam.api.dto.RegistrationResponse;
import com.vitsync.iam.api.mapper.AuthMapper;
import com.vitsync.iam.application.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación: {@code /api/v1/auth/**}.
 *
 * <p>Sin lógica (guía, Fase 4.2): valida el DTO, lo traduce y delega en el caso de uso.
 * {@code @SecurityRequirements()} vacío anula en el contrato el bearer que OpenApiConfig
 * aplica por defecto: estos endpoints se llaman sin token.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, verification, session and password recovery")
@SecurityRequirements
public class AuthController {

    private final RegistrationService registration;
    private final AuthMapper mapper;

    public AuthController(RegistrationService registration, AuthMapper mapper) {
        this.registration = registration;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a patient",
            description = "Creates the account in PENDING_VERIFICATION, together with its patient profile.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or PASSWORD_BREACHED",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "ACCOUNT_ALREADY_EXISTS (does not say which field)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return mapper.toResponse(registration.register(mapper.toCommand(request)));
    }
}
