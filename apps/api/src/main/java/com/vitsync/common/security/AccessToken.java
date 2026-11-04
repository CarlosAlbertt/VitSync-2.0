package com.vitsync.common.security;

import java.time.Instant;

/** Un access token emitido y su caducidad. */
public record AccessToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "AccessToken[expiresAt=" + expiresAt + "]";
    }
}
