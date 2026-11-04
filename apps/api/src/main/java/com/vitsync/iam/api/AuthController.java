package com.vitsync.iam.api;

import com.vitsync.common.error.BusinessException;
import com.vitsync.iam.api.dto.LoginRequest;
import com.vitsync.iam.api.dto.PasswordResetRequest;
import com.vitsync.iam.api.dto.PasswordResetRequestRequest;
import com.vitsync.iam.api.dto.RegisterRequest;
import com.vitsync.iam.api.dto.RegistrationResponse;
import com.vitsync.iam.api.dto.SessionResponse;
import com.vitsync.iam.api.dto.VerifyEmailRequest;
import com.vitsync.iam.api.mapper.AuthMapper;
import com.vitsync.iam.api.mapper.MeMapper;
import com.vitsync.iam.application.EmailVerificationService;
import com.vitsync.iam.application.PasswordResetService;
import com.vitsync.iam.application.RegistrationService;
import com.vitsync.iam.application.Session;
import com.vitsync.iam.application.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final EmailVerificationService verification;
    private final SessionService sessions;
    private final PasswordResetService passwordReset;
    private final AuthMapper mapper;
    private final MeMapper meMapper;
    private final RefreshCookie cookie;

    public AuthController(RegistrationService registration, EmailVerificationService verification,
                          SessionService sessions, PasswordResetService passwordReset,
                          AuthMapper mapper, MeMapper meMapper, RefreshCookie cookie) {
        this.registration = registration;
        this.verification = verification;
        this.sessions = sessions;
        this.passwordReset = passwordReset;
        this.mapper = mapper;
        this.meMapper = meMapper;
        this.cookie = cookie;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a patient",
            description = "Creates the account in PENDING_VERIFICATION, together with its patient profile, "
                    + "and emails a 6-digit verification code valid for 10 minutes.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or PASSWORD_BREACHED",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "ACCOUNT_ALREADY_EXISTS (does not say which field)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return mapper.toResponse(registration.register(mapper.toCommand(request)));
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Verify the email with the emailed code",
            description = "Activates the account. Unknown email, wrong, expired or used code all get the same 400.")
    @ApiResponse(responseCode = "204", description = "Account active")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED or INVALID_OR_EXPIRED_CODE",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "TOO_MANY_ATTEMPTS: the code is burnt; logging in again sends a new one",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public void verify(@Valid @RequestBody VerifyEmailRequest request) {
        verification.verify(request.email(), request.code());
    }

    @PostMapping("/login")
    @Operation(summary = "Log in",
            description = "Returns a 15-minute access token and sets the refresh token in an HttpOnly cookie. "
                    + "Unknown email and wrong password get the same 401. An unverified account gets 403 "
                    + "ACCOUNT_NOT_VERIFIED and a new verification code by email if the previous one is no longer usable.")
    @ApiResponse(responseCode = "200", description = "Session started",
            headers = @Header(name = HttpHeaders.SET_COOKIE,
                    description = RefreshCookie.NAME + "=<opaque>; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth; Max-Age=604800"))
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "ACCOUNT_NOT_VERIFIED or ACCOUNT_DISABLED",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request,
                                                 @RequestHeader(value = HttpHeaders.USER_AGENT, required = false)
                                                 @Parameter(hidden = true) String userAgent) {
        return withSession(sessions.login(request.email(), request.password(), userAgent));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token",
            description = "Exchanges the refresh cookie for a new access token and a new refresh cookie of the same "
                    + "family; the presented token is revoked. Presenting an already used token revokes the whole "
                    + "family (theft signal) and is recorded in audit_logs. Clients must serialize refresh calls.")
    @ApiResponse(responseCode = "200", description = "Session renewed",
            headers = @Header(name = HttpHeaders.SET_COOKIE, description = "The new refresh cookie"))
    @ApiResponse(responseCode = "401", description = "INVALID_REFRESH_TOKEN: missing, expired, revoked or reused; the cookie is cleared",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<SessionResponse> refresh(
            @CookieValue(name = RefreshCookie.NAME, required = false)
            @Parameter(in = ParameterIn.COOKIE, name = RefreshCookie.NAME, hidden = true) String refreshToken,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) @Parameter(hidden = true) String userAgent,
            HttpServletResponse response) {
        try {
            return withSession(sessions.refresh(refreshToken, userAgent));
        } catch (BusinessException e) {
            // Una cookie que ya no sirve se borra: así el cliente no la reenvía en bucle. La
            // cabecera se añade antes de que el handler global escriba el ProblemDetail.
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.clear().toString());
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out",
            description = "Revokes the session of the refresh cookie (its whole token family) and clears the cookie. "
                    + "Always 204, even without a cookie: the result for the client is the same.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookie.NAME, required = false)
            @Parameter(in = ParameterIn.COOKIE, name = RefreshCookie.NAME, hidden = true) String refreshToken) {
        sessions.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.clear().toString()).build();
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request a password reset code",
            description = "Always 202 with an empty body, whether or not the account exists. If it exists and is "
                    + "active, a 6-digit code valid for 15 minutes is emailed (at most one per minute).")
    @ApiResponse(responseCode = "202", description = "Request accepted")
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestRequest request) {
        passwordReset.requestCode(request.email());
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set a new password with the emailed code",
            description = "Changes the password and revokes every session (all refresh token families). "
                    + "Access tokens already issued expire on their own within 15 minutes.")
    @ApiResponse(responseCode = "204", description = "Password changed; log in again")
    @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED, PASSWORD_BREACHED or INVALID_OR_EXPIRED_CODE",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "TOO_MANY_ATTEMPTS: the code is burnt; request a new one",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordReset.reset(request.email(), request.code(), request.newPassword());
    }

    // El refresh va a la cookie; el access token y el usuario, al cuerpo.
    private ResponseEntity<SessionResponse> withSession(Session session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.issue(session.refreshToken()).toString())
                .body(meMapper.toResponse(session));
    }
}
