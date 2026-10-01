# VitSync 2.0

Reconstrucción desde cero de **VitSync**, un SaaS sanitario para gestión de citas,
historial clínico, informes y comunicación paciente-profesional, tratando **datos de
salud (categoría especial, Art. 9 RGPD)**.

Este repositorio arranca vacío a propósito: primero el contrato y el modelo de datos,
después el código. Toda la ingeniería está descrita en la
[guía de construcción](docs/GUIA_CONSTRUCCION.md).

> **v1** vive en [VITSYNC-API](https://github.com/CarlosAlbertt/VITSYNC-API) y
> [VITSYNC-WebApp](https://github.com/CarlosAlbertt/VITSYNC-WebApp). Funcionaba, tenía una
> auditoría de seguridad real (V01–V21) y también deuda estructural. v2 es esa deuda
> resuelta desde el diseño, no parcheada.

---

## Estado

| | |
|---|---|
| Fase actual | **Sprint 0 cerrado** → siguiente: Fase 3, contrato de API |
| Código de producto | Aún no: esqueleto, esquema y entorno |
| Base de datos | Flyway con el esquema base (19 tablas) ✅ |
| Entorno local | `docker compose up -d` → Postgres + MinIO + Mailpit ✅ |
| Tests | `./mvnw verify` en verde, con Testcontainers ✅ |
| Decisiones | Los 8 ADRs escritos ([docs/adr](docs/adr)) ✅ |
| Gestión | Backlog listo para volcar a Jira ([guía](docs/JIRA_SETUP.md)) |

Para retomar el trabajo en cualquier momento: **[docs/HANDOFF.md](docs/HANDOFF.md)**.

## Arrancar en local

Requisitos: **JDK 21** y **Docker** (obligatorio también para los tests: usan
Testcontainers).

```bash
docker compose up -d                                        # Postgres + MinIO + Mailpit
cd apps/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
curl http://localhost:8080/actuator/health                  # → {"status":"UP"}
```

Detalle completo, y qué hacer si ya tienes un PostgreSQL en el 5432, en
[CONTRIBUTING.md](CONTRIBUTING.md).

---

## Stack objetivo

| Capa | Tecnología | Nota |
|---|---|---|
| Base de datos | PostgreSQL 16 | Se mantiene de v1 |
| Migraciones | Flyway | Sustituye a los scripts SQL manuales de v1 |
| Backend | Spring Boot 4.1.1 · Java 21 | Se mantiene de v1; salto a la línea actual ([ADR-0001](docs/adr/0001-stack-tecnologico.md)) |
| Arquitectura | Modular monolith (package-by-feature) + ArchUnit | v1 era package-by-layer |
| API | REST `/api/v1` · OpenAPI 3 (springdoc) · errores RFC 9457 | Contrato primero |
| Seguridad | JWT RS256 (15 min) + refresh opaco rotativo · AES-256-GCM en reposo · auditoría AOP | Diseño heredado de v1, con detección de reuso de token |
| Ficheros | S3-compatible (MinIO en local) + URLs prefirmadas | v1 usaba disco efímero |
| Frontend | **React 19 + TypeScript + Vite 7** | Sustituye a Vue 3 |
| Estado | TanStack Query (servidor) + Zustand (UI) | v1 usaba stores caseros |
| Formularios | React Hook Form + Zod | Esquema compartido con los tipos generados |
| Estilos | Tailwind 4 + shadcn/ui | Tailwind se mantiene |
| Tests | JUnit 5 + Testcontainers · Vitest + Testing Library + MSW · Playwright | v1 testeaba con H2 |
| CI/CD | GitHub Actions (lint, tests, migraciones, gitleaks, drift de OpenAPI) | Nuevo |

El porqué de cada elección está en la guía y, cuando la decisión es discutible, en un
ADR bajo [`docs/adr/`](docs/adr).

---

## Estructura prevista del repositorio

```
vitsync-2.0/
├── apps/
│   ├── api/          Spring Boot (Maven)          ← esqueleto + esquema
│   │   └── src/main/resources/db/migration/       Migraciones Flyway
│   └── web/          React + Vite (pnpm)          ← Fase 9, aún no existe
├── docs/
│   ├── GUIA_CONSTRUCCION.md   Guía completa de las 12 fases
│   ├── HANDOFF.md             Punto de retoma del trabajo
│   ├── SCOPE.md               MVP, v2.1 y fuera de alcance
│   ├── DUDAS_Y_CAMBIOS.md     Cambios estructurales y dudas abiertas
│   ├── JIRA_SETUP.md          Conexión con Jira y volcado del backlog
│   ├── adr/                   Architecture Decision Records (0001–0008)
│   └── openapi.json           Contrato generado (aparecerá en la Fase 3)
├── scripts/jira/     Bootstrap del backlog en Jira
├── .github/workflows/ci.yml   Build, tests y escaneo de secretos
├── docker-compose.yml         Postgres + MinIO + Mailpit
├── CONTRIBUTING.md            Cómo se trabaja aquí
├── Makefile                   Atajos (make up / api / test)
└── .env.example               Variables de entorno, sin valores
```

---

## Documentación

| Documento | Para qué |
|---|---|
| [docs/GUIA_CONSTRUCCION.md](docs/GUIA_CONSTRUCCION.md) | El plan completo: análisis de v1, stack, ERD + DDL, fases 0–12, roadmap |
| [docs/HANDOFF.md](docs/HANDOFF.md) | Dónde está el trabajo y cuál es el siguiente paso |
| [docs/SCOPE.md](docs/SCOPE.md) | Qué entra en el MVP, qué queda para v2.1 y qué no se hace |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Cómo se arranca, se rama, se commitea y qué es "terminado" |
| [docs/JIRA_SETUP.md](docs/JIRA_SETUP.md) | Montar el tablero como en una empresa |
| [docs/DUDAS_Y_CAMBIOS.md](docs/DUDAS_Y_CAMBIOS.md) | Cambios estructurales del repo y dudas abiertas |
| [docs/adr/](docs/adr) | Decisiones de arquitectura, una por fichero |
| [docs/VADEMECUM.md](docs/VADEMECUM.md) | Glosario técnico: 193 términos de ingeniería de software y AI engineering |

---

## Roadmap

Nueve sprints de una semana, cada uno con algo demostrable:

`S0` kickoff e infraestructura · `S1` autenticación · `S2` catálogo · `S3` reserva de citas ·
`S4` perfil y datos clínicos · `S5` informes y ficheros · `S6` panel admin · `S7` RGPD ·
`S8` calidad y producción.

Detalle en la [guía](docs/GUIA_CONSTRUCCION.md#roadmap-por-sprints).

---

## Licencia

Pendiente de decidir (previsto MIT).
