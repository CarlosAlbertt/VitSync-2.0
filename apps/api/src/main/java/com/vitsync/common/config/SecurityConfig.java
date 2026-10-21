package com.vitsync.common.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * de forma explícita. El login con JWT llega en la primera slice del Sprint 1; mientras
 * tanto, cualquier ruta protegida responde 401.
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
                            @Qualifier("handlerExceptionResolver")HandlerExceptionResolver resolver) throws Exception {
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
                        // Todo lo demás, con usuario autenticado.
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        // Sin autenticar -> se pasa la excepción al handler global (401).
                        .authenticationEntryPoint((req, res, ex) -> resolver.resolveException(req, res, null, ex))
                        .accessDeniedHandler(((request, response, accessDeniedException) -> resolver.resolveException(request, response, null, accessDeniedException)))
                );

        return http.build();
    }
}
