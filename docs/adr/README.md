# Architecture Decision Records

Una decisión por fichero, numerada y nunca borrada: si algo cambia, se escribe un ADR
nuevo que sustituye al anterior y el viejo queda marcado como *Sustituido*.

Plantilla: [`0000-template.md`](0000-template.md).

## ADRs previstos (Fase 0 de la guía)

| ADR | Decisión | Estado |
|---|---|---|
| 0001 | Stack tecnológico de v2 | Pendiente de redactar |
| 0002 | Monorepo frente a dos repositorios | Pendiente |
| 0003 | Sin herencia JPA en el modelo de usuario | Pendiente |
| 0004 | Estrategia de autenticación (RS256 + refresh rotativo) | Pendiente |
| 0005 | Cifrado en reposo y blind index | Pendiente |
| 0006 | Almacenamiento de ficheros S3-compatible | Pendiente |
| 0007 | Estrategia de tiempo real | Pendiente |
| 0008 | Estrategia de pruebas (Testcontainers) | Pendiente |

Las decisiones ya están tomadas y resumidas en [`../HANDOFF.md`](../HANDOFF.md#3-decisiones-ya-cerradas-no-volver-a-abrirlas-sin-un-adr);
falta redactarlas con contexto y consecuencias.
