package com.vitsync.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Comprueba el cableado con Spring sin levantar la aplicación entera (ni BD ni Docker).
 *
 * <p>{@link ApplicationContextRunner} crea un contexto mínimo solo con {@link CryptoConfig}
 * y las propiedades que se le pasan. Así se demuestra el criterio de ADR-0005 "la aplicación
 * no arranca si falta alguna clave" en milisegundos.
 */
class CryptoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CryptoConfig.class);

    @Test
    void context_starts_with_a_valid_configuration() {
        runner.withPropertyValues(
                        "vitsync.crypto.key=" + TestKeys.randomKey(),
                        "vitsync.crypto.key-version=v3",
                        "vitsync.crypto.blind-index-pepper=" + TestKeys.PEPPER,
                        "vitsync.crypto.previous-keys.v2=" + TestKeys.randomKey())
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed()
                            .hasSingleBean(FieldEncryptor.class)
                            .hasSingleBean(BlindIndex.class);
                    FieldEncryptor encryptor = ctx.getBean(FieldEncryptor.class);
                    assertThat(encryptor.encrypt("x")).startsWith("v3:");
                    assertThat(ctx.getBean(KeyRing.class).keyFor("v2")).isPresent();
                });
    }

    @Test
    void context_fails_when_the_key_is_missing() {
        runner.withPropertyValues(
                        "vitsync.crypto.key-version=v1",
                        "vitsync.crypto.blind-index-pepper=" + TestKeys.PEPPER)
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause()
                        .isInstanceOf(EncryptionException.class)
                        .hasMessageContaining("ENCRYPTION_KEY"));
    }

    @Test
    void context_fails_when_the_pepper_is_missing() {
        runner.withPropertyValues(
                        "vitsync.crypto.key=" + TestKeys.randomKey(),
                        "vitsync.crypto.key-version=v1")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause()
                        .hasMessageContaining("BLIND_INDEX_PEPPER"));
    }

    @Test
    void context_fails_with_the_dev_key_unless_allowed() {
        String[] devConfig = {
                "vitsync.crypto.key=" + TestKeys.ZERO_KEY,
                "vitsync.crypto.key-version=v1",
                "vitsync.crypto.blind-index-pepper=" + TestKeys.PEPPER};

        runner.withPropertyValues(devConfig)
                .run(ctx -> assertThat(ctx).hasFailed());
        runner.withPropertyValues(devConfig)
                .withPropertyValues("vitsync.crypto.allow-insecure-dev-key=true")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }
}
