package com.vitsync.iam.api;

import com.vitsync.iam.api.dto.MeResponse;
import com.vitsync.iam.api.dto.UpdateMeRequest;
import com.vitsync.iam.api.mapper.MeMapper;
import com.vitsync.iam.application.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El usuario autenticado: {@code /api/v1/me}. Exige Bearer (regla por defecto de
 * SecurityConfig). El usuario sale del {@code sub} del token, nunca de la URL.
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Me", description = "The authenticated user")
public class MeController {

    private final ProfileService profiles;
    private final MeMapper mapper;

    public MeController(ProfileService profiles, MeMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user")
    @ApiResponse(responseCode = "200", description = "The user")
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(profiles.me(UUID.fromString(jwt.getSubject())));
    }

    @PatchMapping
    @Operation(summary = "Update the authenticated user's own data",
            description = "Absent fields are left unchanged. Email, NIF and role cannot be changed here.")
    @ApiResponse(responseCode = "200", description = "The updated user")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public MeResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateMeRequest request) {
        return mapper.toResponse(profiles.update(UUID.fromString(jwt.getSubject()),
                request.firstName(), request.lastName(), request.phone(), request.locale()));
    }
}
