package com.vitsync.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** Emisión y verificación de access tokens, sin Spring: claves efímeras y reloj real. */
class AccessTokenServiceTest {

    private final JwtProperties props = new JwtProperties(null, null, "vitsync-api", Duration.ofMinutes(15), true);
    private final JwtConfig config = new JwtConfig();
    private final JwtKeys keys = JwtKeys.ephemeral();
    private final AccessTokenService service =
            new AccessTokenService(config.jwtEncoder(keys), props, Clock.systemUTC());
    private final JwtDecoder decoder = config.jwtDecoder(keys, props);

    @Test
    void token_carries_minimal_claims_and_lives_15_minutes() {
        UUID subject = UUID.randomUUID();
        AccessToken token = service.issue(subject, "PATIENT");

        Jwt jwt = decoder.decode(token.value());
        assertThat(jwt.getSubject()).isEqualTo(subject.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("PATIENT");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("vitsync-api");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
        // Nada personal en el token: se lee con un simple base64.
        assertThat(jwt.getClaims()).containsOnlyKeys("iss", "sub", "jti", "iat", "exp", "role");
    }

    @Test
    void token_signed_with_another_key_is_rejected() {
        AccessTokenService other = new AccessTokenService(config.jwtEncoder(JwtKeys.ephemeral()), props, Clock.systemUTC());
        String forged = other.issue(UUID.randomUUID(), "ADMIN").value();

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void unsigned_token_is_rejected() {
        // El ataque clásico: "alg":"none" y sin firma.
        String header = b64("{\"alg\":\"none\"}");
        String payload = b64("{\"sub\":\"" + UUID.randomUUID() + "\",\"role\":\"ADMIN\",\"iss\":\"vitsync-api\"}");

        assertThatThrownBy(() -> decoder.decode(header + "." + payload + ".")).isInstanceOf(JwtException.class);
    }

    @Test
    void expired_token_is_rejected() {
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String expired = new AccessTokenService(config.jwtEncoder(keys), props, past)
                .issue(UUID.randomUUID(), "PATIENT").value();

        assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(JwtException.class);
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
}
