# HANDOFF — VitSync 2.0

> Documento de retoma. Léelo entero antes de tocar nada: en 5 minutos sabes dónde está
> el proyecto, qué está decidido y cuál es el siguiente paso concreto.
> Última actualización: **2026-09-12**.

---

## 1. Qué es esto

Reconstrucción desde cero de VitSync (SaaS sanitario) con objetivo **portfolio**: demostrar
el proceso completo de ingeniería que se sigue en una empresa, no solo el producto final.

- **v1**: [VITSYNC-API](https://github.com/CarlosAlbertt/VITSYNC-API) (Spring Boot + Vue).
  Funcional, con auditoría de seguridad propia (V01–V21) y deuda estructural documentada.
- **v2 (este repo)**: mismo dominio, mismas bases (PostgreSQL + Spring Boot), frontend
  migrado a React, y todo lo demás rehecho según [la guía](GUIA_CONSTRUCCION.md).

---

## 2. Estado actual (2026-09-12)

**Sprint 0 cerrado.** El criterio que fijaba este documento se cumple: en la máquina,
`docker compose up -d && ./mvnw verify` pasa en verde y `/actuator/health` responde `UP`.

### Hecho
- Análisis completo de los dos repos de v1 (código, no solo README).
- Inventario de problemas estructurales de v1 con evidencia (P1–P13 en la guía, Parte A).
- Stack de v2 decidido y justificado línea por línea (Parte B).
- Modelo de datos de v2 diseñado: ERD + DDL completo (Fase 1).
- Plan de las 12 fases y roadmap de 9 sprints.
- **Alcance cerrado** en [SCOPE.md](SCOPE.md): MVP, v2.1 y fuera, con escenarios Gherkin.
- **Los ocho ADRs escritos y aceptados** ([`adr/`](adr)). Con ellos se cierran las dudas de
  la versión de Spring Boot (4.1.1) y del monorepo.
- **Entorno local reproducible**: `docker-compose.yml` con Postgres 16 + MinIO + Mailpit,
  los tres con healthcheck y el bucket creado al arrancar.
- **Flyway con `V1__baseline_schema.sql`**: las 19 tablas del dominio, aplicadas y
  verificadas contra PostgreSQL real.
- **Testcontainers** en la suite: `./mvnw verify` levanta `postgres:16-alpine`, aplica las
  migraciones desde cero y valida el mapeo de Hibernate.
- **CI mínima** en GitHub Actions: build + tests + gitleaks.
- **Convenciones escritas**: [CONTRIBUTING.md](../CONTRIBUTING.md), plantilla de PR con la
  Definition of Done, `Makefile` y `.env.example`.

### No hecho todavía
- **Cero código de producto**: `apps/api` tiene el esqueleto y la configuración, ninguna
  entidad ni endpoint. No existe `apps/web` (Fase 9).
- Sin tablero de Jira (guía lista en [JIRA_SETUP.md](JIRA_SETUP.md), falta ejecutarla).
- `master` sin reglas de protección configuradas en GitHub.
- Sin licencia (ver [DUDAS_Y_CAMBIOS](DUDAS_Y_CAMBIOS.md), Q7).
- La CI no comprueba aún formato, cobertura, vulnerabilidades de dependencias ni drift de
  OpenAPI: esas herramientas entran en la Fase 11, según se configuren en el proyecto.

---

## 3. Decisiones ya cerradas (no volver a abrirlas sin un ADR)

| # | Decisión | Motivo corto |
|---|---|---|
| D1 | PostgreSQL y Spring Boot / Java 21 se mantienen | Base sólida de v1 y objetivo de aprendizaje |
| D2 | Frontend Vue → **React 19 + TypeScript** | Petición explícita + demanda del mercado |
| D3 | **Flyway** desde el primer commit; `ddl-auto=validate` en todos los perfiles | v1 aplicaba SQL a mano en Neon |
| D4 | **Sin herencia JPA** en usuarios: `users` + `patient_profiles` / `doctor_profiles` | La herencia `JOINED` de v1 causó médicos huérfanos (FIX01) |
| D5 | DTOs + MapStruct; la entidad nunca cruza el controlador | v1 serializaba entidades y parcheaba con Jackson |
| D6 | PK `bigint` interna + `public_id` UUID expuesto | Evita enumeración e IDOR por conteo |
| D7 | Datos clínicos cifrados AES-256-GCM + blind index donde haya que buscar | Art. 32 RGPD; se hereda de v1 |
| D8 | Ficheros en S3-compatible (MinIO local) con URLs prefirmadas | El disco de Render es efímero |
| D9 | **Testcontainers**, nunca H2 | v1 testeaba contra una BD distinta a producción |
| D10 | Contrato **OpenAPI primero**; el cliente TS del front se genera de la spec | Evita el drift front/back de v1 |
| D11 | Estado de servidor en el front con **TanStack Query**; Zustand solo para UI | v1 tenía stores caseros |
| D12 | Idioma: código, BD, ramas y commits en **inglés**; UI y textos en español | v1 mezclaba y lo parcheaba con anotaciones |
| D13 | Trunk-based: `main` protegida, ramas cortas, squash merge, CI verde obligatoria | |

---

Las decisiones D1–D13 están ahora redactadas como ADRs, con contexto, alternativas
descartadas y consecuencias: ver [`docs/adr/`](adr).

## 4. Decisiones pendientes (bloquean fases concretas)

| Pendiente | Bloquea | Opciones |
|---|---|---|
| Gestor de tareas definitivo | Sprint 1 | Jira Cloud (recomendado, guía lista) · GitHub Projects |
| Proveedor de despliegue | Sprint 8 | Render/Fly (API) + Vercel (web) + Neon (BD) |
| Licencia | — | MIT previsto, sin fichero `LICENSE` todavía |

Cerradas desde la última revisión: **monorepo** ([ADR-0002](adr/0002-monorepo-frente-a-dos-repositorios.md)),
**tiempo real** ([ADR-0007](adr/0007-estrategia-de-tiempo-real.md)) y **versión de Spring
Boot** ([ADR-0001](adr/0001-stack-tecnologico.md)).

---

## 5. Siguiente paso concreto

El Sprint 0 está cerrado. **Sprint 1 — Autenticación**, en este orden:

1. **Acciones manuales pendientes del Sprint 0** (no las puede hacer una sesión de Claude):
   - Montar el tablero siguiendo [JIRA_SETUP.md](JIRA_SETUP.md) y volcar
     `scripts/jira/backlog.json` (9 épicas, con criterios de aceptación).
   - Configurar la protección de `master` en GitHub: PR obligatorio, CI verde,
     historial lineal, sin force-push.
2. **Contrato antes que código** (guía, Fase 3): añadir springdoc — fijando la línea
   compatible con Boot 4, que es lo único a verificar del [ADR-0001](adr/0001-stack-tecnologico.md) —,
   las convenciones REST y el manejo central de errores con `ProblemDetail` (RFC 9457).
3. **Estructura del backend** (guía, Fase 4): package-by-feature bajo `com.vitsync`, con el
   test de ArchUnit que impide que un módulo importe los internos de otro.
4. **Primera vertical slice: registro y login.** Entidad `User` sin herencia
   ([ADR-0003](adr/0003-modelo-de-usuario-sin-herencia-jpa.md)), el `AttributeConverter` de
   cifrado con prefijo de versión ([ADR-0005](adr/0005-cifrado-en-reposo-y-blind-index.md)),
   y la rotación de refresh con detección de reuso
   ([ADR-0004](adr/0004-autenticacion-jwt-rs256-con-refresh-rotativo.md)).

Los criterios de aceptación de esa slice ya están escritos en
[SCOPE.md](SCOPE.md#sesión-y-refresh): cada escenario Gherkin debe acabar siendo un test con
un nombre reconocible.

**Cómo arrancar el entorno**: [CONTRIBUTING.md](../CONTRIBUTING.md#2-arrancar-el-proyecto).

---

## 6. Cómo retomar el trabajo con Claude

Pega esto al empezar una sesión nueva:

```
Retomamos VitSync 2.0. Lee docs/HANDOFF.md y docs/GUIA_CONSTRUCCION.md de
https://github.com/CarlosAlbertt/VitSync-2.0 y continúa por el "siguiente paso concreto".
Actúa como jefe de proyecto: propón el plan del sprint antes de escribir código.
```

Reglas de trabajo acordadas:
- Nada se mergea sin cumplir la Definition of Done (guía, Anexo A).
- Ninguna migración Flyway se edita después de mergearse: se corrige con otra nueva.
- Cero cambios "temporales" en `main` (fue el error visible de v1).
- Cada decisión discutible se cierra con un ADR, no con un mensaje de chat.

---

## 7. Mapa rápido de la guía

| Necesito… | Ir a |
|---|---|
| Saber qué falló en v1 | Parte A |
| Justificar una tecnología | Parte B |
| El esquema de BD y el DDL | Fase 1 |
| Levantar el entorno local | Fase 2 |
| Convenciones de API y errores | Fase 3 |
| Estructura de paquetes del backend | Fase 4 |
| Un ejemplo completo de funcionalidad | Fase 5 (reservar cita) |
| Seguridad y RGPD | Fase 6 |
| Estructura del frontend React | Fase 9 |
| Qué testear y con qué | Fase 10 |
| Pipeline de CI | Fase 11 |
| Qué enseñar en el portfolio | Fase 12 |
