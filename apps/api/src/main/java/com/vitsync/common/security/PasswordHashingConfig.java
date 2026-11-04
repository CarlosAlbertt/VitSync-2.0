package com.vitsync.common.security;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hash de contraseñas: <b>BCrypt con coste 12</b> (ADR-0004).
 *
 * <p>Por qué BCrypt y no Argon2id, aunque Argon2id sea el recomendado hoy por OWASP:
 * <ul>
 *   <li>Es lo que fija el ADR-0004, y cambiarlo sin motivo de seguridad concreto sería
 *       reabrir una decisión cerrada.
 *   <li>Spring Security implementa Argon2 sobre BouncyCastle: una dependencia criptográfica
 *       más que mantener. BCrypt viene en {@code spring-security-crypto}.
 *   <li>Coste 12 son ~250 ms por hash en un portátil: lento para un atacante, aceptable en
 *       un login.
 * </ul>
 *
 * <p>El coste se puede bajar con {@code vitsync.security.bcrypt-cost} <b>solo en los tests</b>
 * (allí vale 4): cada registro de un test costaría si no un cuarto de segundo, y la suite
 * entera se volvería lenta sin probar nada más.
 *
 * <p>Se envuelve en {@link DelegatingPasswordEncoder}: el hash guardado empieza por
 * {@code {bcrypt}}. Si un día se migra a Argon2id, los hashes viejos se siguen verificando y
 * se pueden rehacer en el siguiente login, sin forzar a nadie a cambiar la contraseña.
 */
@Configuration(proxyBeanMethods = false)
class PasswordHashingConfig {

    @Bean
    PasswordEncoder passwordEncoder(@Value("${vitsync.security.bcrypt-cost:12}") int cost) {
        return new DelegatingPasswordEncoder("bcrypt",
                Map.of("bcrypt", new BCryptPasswordEncoder(cost)));
    }
}
