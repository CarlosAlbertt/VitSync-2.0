package com.vitsync.iam.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sesión iniciada ({@code Session} en el frontend). El refresh token NO va aquí: viaja en la
 * cookie {@code HttpOnly}, inaccesible para JavaScript (ADR-0004). El access token se guarda
 * solo en memoria del navegador.
 */
public record SessionResponse(
        @Schema(description = "JWT RS256, valid for 15 minutes. Send it as 'Authorization: Bearer <token>'")
        String accessToken,
        MeResponse user) {

    @Override
    public String toString() {
        return "SessionResponse[user=" + user.id() + "]";
    }
}
