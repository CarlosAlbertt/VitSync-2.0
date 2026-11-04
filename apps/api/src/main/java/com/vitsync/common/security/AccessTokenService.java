package com.vitsync.common.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite access tokens JWT firmados con RS256 (ADR-0004).
 *
 * <p>Claims mínimos: {@code sub} = {@code public_id} del usuario (nunca el id interno, D6),
 * {@code role}, {@code jti} (id único del token), {@code iss}, {@code iat} y {@code exp}. Nada
 * de nombre, email ni datos clínicos: un JWT se lee con un simple base64, no está cifrado.
 */
@Service
public class AccessTokenService {

    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final Clock clock;

    public AccessTokenService(JwtEncoder encoder, JwtProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    public AccessToken issue(UUID subject, String role) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(props.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(subject.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLE_CLAIM, role)
                .build();
        // RS256 explícito: nunca se deja que el algoritmo lo decida otro.
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, expiresAt);
    }
}
