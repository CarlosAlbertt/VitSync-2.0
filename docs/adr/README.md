# Architecture Decision Records

Una decisión por fichero, numerada y nunca borrada: si algo cambia, se escribe un ADR
nuevo que sustituye al anterior y el viejo queda marcado como *Sustituido*.

Plantilla: [`0000-template.md`](0000-template.md).

## ADRs escritos

| ADR | Decisión | Estado |
|---|---|---|
| [0001](0001-stack-tecnologico.md) | Stack tecnológico de v2 (Spring Boot 4.1.1 · Java 21 · React 19) | Aceptado |
| [0002](0002-monorepo-frente-a-dos-repositorios.md) | Monorepo frente a dos repositorios | Aceptado |
| [0003](0003-modelo-de-usuario-sin-herencia-jpa.md) | Sin herencia JPA en el modelo de usuario | Aceptado |
| [0004](0004-autenticacion-jwt-rs256-con-refresh-rotativo.md) | JWT RS256 + refresh opaco rotativo con detección de reuso | Aceptado |
| [0005](0005-cifrado-en-reposo-y-blind-index.md) | Cifrado en reposo AES-256-GCM y blind index | Aceptado |
| [0006](0006-almacenamiento-de-ficheros-s3.md) | Ficheros en S3-compatible con URLs prefirmadas | Aceptado |
| [0007](0007-estrategia-de-tiempo-real.md) | SSE para notificaciones, STOMP para el chat | Aceptado |
| [0008](0008-estrategia-de-pruebas.md) | Testcontainers en vez de H2, y pirámide de pruebas | Aceptado |

Las decisiones de partida están resumidas en el
[HANDOFF §3](../HANDOFF.md#3-decisiones-ya-cerradas-no-volver-a-abrirlas-sin-un-adr);
estos ADRs son su versión redactada, con contexto, alternativas y consecuencias.

## Candidatos a ADR futuro

Se escribirán cuando la fase correspondiente los active:

| Tema | Fase |
|---|---|
| Convenciones de API y formato de errores (RFC 9457) | 3 |
| Estructura modular del backend y reglas de ArchUnit | 4 |
| Proveedor de despliegue y estrategia de entornos | 8 |
| Licencia del proyecto | — |
