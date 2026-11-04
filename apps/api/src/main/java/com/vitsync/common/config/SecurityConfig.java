package com.vitsync.common.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Configuración base de Spring Security.
 *
 * <p>Principio: cerrado por defecto. Todo exige autenticación salvo lo que se abre aquí
 * de forma explícita.
 *
 * <p>Autenticación: <b>JWT RS256 en la cabecera {@code Authorization: Bearer}</b> (ADR-0004).
 * El filtro Bearer de Spring Security ({@code oauth2ResourceServer().jwt()}) valida firma,
 * algoritmo, emisor y caducidad con el {@code JwtDecoder} de {@code JwtConfig}, sin tocar la
 * BD. Un token ausente, caducado o manipulado acaba en el mismo 401 con formato RFC 9457.
 *
 * <p>{@code @EnableMethodSecurity} activa {@code @PreAuthorize} en los métodos, el segundo
 * nivel de autorización de la guía (Fase 6.2).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Un SecurityFilterChain es la lista de filtros de seguridad que atraviesa cada petición.
    // Al declarar este bean, Spring Boot deja de aplicar su configuración por defecto.
    // El resolver es el mismo que usa Spring MVC: con él, los errores de seguridad acaban
    // en GlobalExceptionHandler y tienen el mismo formato que el resto.
    @Bean
    SecurityFilterChain api(HttpSecurity http,
                            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
                            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        // Sin autenticar o con token inválido -> se pasa la excepción al handler global (401).
        AuthenticationEntryPoint unauthenticated = (req, res, ex) -> resolver.resolveException(req, res, null, ex);
        http
                // API sin estado con Bearer: no hay sesión que un CSRF pueda aprovechar.
                .csrf(AbstractHttpConfigurer::disable)
                // Fuera los mecanismos pensados para webs con formulario: no los usamos.
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // STATELESS: el servidor no guarda sesión; cada petición trae su token.
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Reglas por ruta: se evalúan en orden y gana la primera que encaja.
                .authorizeHttpRequests(auth -> auth
                        // Públicas: health (para el balanceador/monitorización) y la documentación.
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        // Autenticación: se llama sin token, por definición. Se abre endpoint
                        // a endpoint y solo con POST; lo que no esté en la lista sigue cerrado.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register",
                                "/api/v1/auth/verify", "/api/v1/auth/login", "/api/v1/auth/refresh",
                                // logout funciona solo con la cookie: así se puede cerrar
                                // sesión aunque el access token ya haya caducado.
                                "/api/v1/auth/logout").permitAll()
                        // Todo lo demás, con usuario autenticado.
                        .anyRequest().authenticated())
                // Bearer JWT. El entry point propio sustituye al de Spring, que respondería un
                // 401 vacío con WWW-Authenticate: así también estos 401 son ProblemDetail.
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(unauthenticated))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(((request, response, accessDeniedException) -> resolver.resolveException(request, response, null, accessDeniedException)))
                );

        return http.build();
    }
}
