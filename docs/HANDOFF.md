# HANDOFF — VitSync 2.0

> Documento de retoma. Léelo entero antes de tocar nada: en 5 minutos sabes dónde está
> el proyecto, qué está decidido y cuál es el siguiente paso concreto.
> Última actualización: **2026-08-20**.

---

## 1. Qué es esto

Reconstrucción desde cero de VitSync (SaaS sanitario) con objetivo **portfolio**: demostrar
el proceso completo de ingeniería que se sigue en una empresa, no solo el producto final.

- **v1**: [VITSYNC-API](https://github.com/CarlosAlbertt/VITSYNC-API) (Spring Boot + Vue).
  Funcional, con auditoría de seguridad propia (V01–V21) y deuda estructural documentada.
- **v2 (este repo)**: mismo dominio, mismas bases (PostgreSQL + Spring Boot), frontend
  migrado a React, y todo lo demás rehecho según [la guía](GUIA_CONSTRUCCION.md).

---

## 2. Estado actual (2026-08-20)

### Hecho
- Análisis completo de los dos repos de v1 (código, no solo README).
- Inventario de problemas estructurales de v1 con evidencia (P1–P13 en la guía, Parte A).
- Stack de v2 decidido y justificado línea por línea (Parte B).
- Modelo de datos de v2 diseñado: ERD + DDL completo (Fase 1).
- Plan de las 12 fases y roadmap de 9 sprints.
- Repositorio creado con documentación base.

### No hecho todavía
- Cero código de producto. No hay `apps/api` ni `apps/web`.
- Sin ADRs escritos (solo la carpeta y la plantilla).
- Sin tablero de Jira (guía lista en [JIRA_SETUP.md](JIRA_SETUP.md), falta ejecutarla).
- Sin `docker-compose.yml`, sin Flyway, sin CI.

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

## 4. Decisiones pendientes (bloquean fases concretas)

| Pendiente | Bloquea | Opciones |
|---|---|---|
| Monorepo (`apps/api` + `apps/web`) vs dos repos | Fase 4 | **Recomendado: monorepo**, ya reflejado en el README |
| Gestor de tareas definitivo | Sprint 0 | Jira Cloud (recomendado) · GitHub Projects |
| Estrategia de tiempo real | Sprint 7+ | SSE para notificaciones + STOMP para chat |
| Proveedor de despliegue | Sprint 8 | Render/Fly (API) + Vercel (web) + Neon (BD) |
| Licencia | — | MIT previsto |

---

## 5. Siguiente paso concreto

**Sprint 0, en este orden:**

1. **Montar el tablero** siguiendo [JIRA_SETUP.md](JIRA_SETUP.md) y volcar
   `scripts/jira/backlog.json` (9 épicas, historias con criterios de aceptación).
2. **Escribir los ADRs 0001–0008** (lista en la guía, Fase 0.2). La plantilla está en
   `docs/adr/0000-template.md`. Las decisiones ya están tomadas: solo hay que redactarlas.
3. **Crear `docker-compose.yml`** con Postgres 16 + MinIO + Mailpit (guía, Fase 2.1).
4. **Esqueleto del backend** con Spring Initializr y la migración `V1__baseline_schema.sql`
   copiada del DDL de la guía (Fase 1.4).
5. **CI mínima**: build + tests en GitHub Actions, y protección de `main`.

Criterio para dar el Sprint 0 por cerrado: en una máquina limpia,
`docker compose up -d && ./mvnw verify` pasa en verde y `/actuator/health` responde.

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
