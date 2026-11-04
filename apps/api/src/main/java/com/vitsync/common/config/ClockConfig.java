package com.vitsync.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación como bean.
 *
 * <p>Las reglas con tiempo (caducidad de códigos, backoff, expiración de tokens) piden la
 * hora a este {@link Clock} en vez de a {@code Instant.now()}. En producción es el reloj del
 * sistema en UTC; un test puede sustituirlo por uno fijo y probar "qué pasa dentro de 11
 * minutos" sin esperar 11 minutos.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
