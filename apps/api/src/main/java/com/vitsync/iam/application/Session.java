package com.vitsync.iam.application;

/**
 * Resultado de un login (o, más adelante, de un refresh): el access token para la respuesta,
 * el refresh token para la cookie y los datos del usuario.
 */
public record Session(String accessToken, String refreshToken, MeView user) {

    @Override
    public String toString() {
        return "Session[user=" + user.id() + "]";
    }
}
