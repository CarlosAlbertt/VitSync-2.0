package com.vitsync.iam.infrastructure;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Elige la implementación de {@link BreachedPasswordChecker}.
 *
 * <p>Con {@code vitsync.hibp.enabled=false} (los tests) no se hace ninguna llamada de red:
 * una suite que depende de internet falla por motivos que no tienen que ver con el código.
 *
 * <p>Timeouts cortos a propósito: el usuario está esperando la respuesta del registro, y
 * si HaveIBeenPwned tarda más de un par de segundos se sigue sin él.
 */
@Configuration(proxyBeanMethods = false)
class BreachedPasswordConfig {

    @Bean
    BreachedPasswordChecker breachedPasswordChecker(
            @Value("${vitsync.hibp.enabled:true}") boolean enabled,
            @Value("${vitsync.hibp.base-url:https://api.pwnedpasswords.com}") String baseUrl,
            @Value("${vitsync.hibp.timeout:2s}") Duration timeout) {
        if (!enabled) {
            return password -> false;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new PwnedPasswordsClient(RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                // HaveIBeenPwned exige un User-Agent que identifique a la aplicación.
                .defaultHeader("User-Agent", "vitsync-api")
                .build());
    }
}
