package com.vitsync.common.crypto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra las piezas del cifrado como beans.
 *
 * <p>{@link KeyRing} y {@link BlindIndex} validan su configuración en el constructor: como
 * son singletons que Spring crea al arrancar, un error de claves para el arranque entero en
 * vez de aparecer con la primera petición.
 *
 * <p>Las clases de cifrado no llevan anotaciones de Spring a propósito: así se prueban con un
 * {@code new} en tests unitarios, sin levantar ningún contexto.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfig {

    @Bean
    KeyRing keyRing(CryptoProperties props) {
        return new KeyRing(props);
    }

    @Bean
    FieldEncryptor fieldEncryptor(KeyRing keyRing) {
        return new FieldEncryptor(keyRing);
    }

    @Bean
    BlindIndex blindIndex(CryptoProperties props) {
        return new BlindIndex(props.blindIndexPepper());
    }
}
