package com.vitsync.common.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Piezas del JWT: claves, firma (encoder), verificación (decoder) y cómo se convierte un token
 * válido en el usuario autenticado de Spring Security.
 *
 * <p>El decoder lo usa el filtro Bearer de Spring Security en cada petición
 * ({@code oauth2ResourceServer().jwt()} en {@code SecurityConfig}): comprueba la firma con la
 * clave pública, que el algoritmo sea RS256, el emisor y la caducidad. Sin consultar la BD.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    JwtKeys jwtKeys(JwtProperties props) {
        return JwtKeys.from(props);
    }

    @Bean
    JwtEncoder jwtEncoder(JwtKeys keys) {
        RSAKey jwk = new RSAKey.Builder(keys.publicKey()).privateKey(keys.privateKey()).keyID(keys.keyId()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtKeys keys, JwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey())
                // Solo RS256: cierra el ataque clásico de cambiar "alg" a "none" o a HS256.
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), new JwtIssuerValidator(props.issuer())));
        return decoder;
    }

    /**
     * Token válido → autenticación con nombre = {@code sub} (el {@code public_id}) y una
     * autoridad {@code ROLE_<role>}, la que esperan {@code hasRole(...)} y {@code @PreAuthorize}.
     */
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(AccessTokenService.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
