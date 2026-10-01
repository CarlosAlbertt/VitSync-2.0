package com.vitsync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prueba de humo del contexto. Aunque parezca trivial, con Flyway y
 * {@code ddl-auto=validate} cubre tres cosas a la vez:
 *
 * <ul>
 *   <li>las migraciones aplican desde cero sobre un PostgreSQL limpio,
 *   <li>el mapeo de Hibernate concuerda con el esquema que deja Flyway,
 *   <li>el contexto de Spring levanta con la configuración real.
 * </ul>
 *
 * Contra PostgreSQL de verdad, nunca H2 (ADR-0008).
 */
@SpringBootTest
@Testcontainers
class VitSyncApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16-alpine");

	@Test
	void contextLoads() {
	}

}
