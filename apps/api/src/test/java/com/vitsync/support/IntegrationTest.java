package com.vitsync.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de los tests de integración: aplicación completa + PostgreSQL real (ADR-0008).
 *
 * <p>El contenedor es estático y se arranca una sola vez para toda la ejecución (patrón
 * "singleton container"), no uno por clase: es la mitigación que pide el ADR-0008 para que la
 * suite no tarde más por cada test nuevo. Testcontainers lo para al acabar la JVM.
 *
 * <p>Los tests no borran datos entre sí: cada uno usa emails y NIF aleatorios
 * ({@link TestData}), así no dependen del orden ni se pisan.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
