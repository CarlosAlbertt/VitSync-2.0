# ADR-0002: Monorepo frente a dos repositorios

## Estado

**Aceptado** — 2026-09-12

## Contexto

v1 vivía en dos repositorios independientes: `VITSYNC-API` (Spring Boot) y `VITSYNC-WebApp`
(Vue). El contrato entre ambos era `docs/API_REFERENCE.md`, escrito a mano en el repo del
backend y copiado mentalmente al del frontend. El resultado fue el problema P6 de la guía:
la documentación se desincronizó del código y el frontend descubría los cambios de contrato
al romperse en ejecución.

v2 mantiene la separación backend/frontend, pero el contrato deja de ser un documento y pasa
a ser un artefacto generado (OpenAPI 3) del que se deriva el cliente TypeScript. Eso cambia
la pregunta: ya no es "dónde guardo el código", sino **"dónde vive el contrato y quién
garantiza que las dos partes usan la misma versión"**.

La duda estaba abierta como Q6 en [`DUDAS_Y_CAMBIOS.md`](../DUDAS_Y_CAMBIOS.md) y como
pendiente en el [HANDOFF §4](../HANDOFF.md).

## Opciones consideradas

1. **Monorepo** — `apps/api` (Maven) + `apps/web` (pnpm), un solo repositorio.
   - `+` `docs/openapi.json` vive en el mismo árbol que lo produce y que lo consume: un PR
     que cambia el contrato enseña, en el mismo diff, el endpoint nuevo y el cliente
     regenerado. El drift deja de ser posible por construcción.
   - `+` Un cambio que toca las dos partes es **un** PR con **una** CI, no dos PR que hay que
     mergear en el orden correcto.
   - `+` Un solo sitio donde mirar: issues, tablero, ADRs e historial. Para portfolio, el
     revisor abre un enlace, no tres.
   - `+` Demuestra manejo de tooling de monorepo, que es lo que usan hoy la mayoría de los
     equipos de producto.
   - `−` La CI tiene que filtrar por rutas para no ejecutar los tests del front cuando solo
     cambia el backend.
   - `−` El historial de git mezcla dos tecnologías.
   - `−` Si algún día se despliegan por separado con herramientas que asumen "un repo, un
     servicio", hay que configurar el directorio raíz en cada una (Render y Vercel lo
     soportan).

2. **Dos repositorios** — como en v1.
   - `+` Separación física total; cada repo tiene su CI y su ciclo de vida.
   - `+` Encaja sin fricción con cualquier plataforma de despliegue.
   - `−` El contrato hay que sincronizarlo: o se publica el OpenAPI como paquete versionado,
     o se copia a mano. Lo primero es infraestructura que este proyecto no necesita; lo
     segundo es exactamente el error de v1.
   - `−` Un cambio de contrato son dos PR coordinados.

## Decisión

**Monorepo.** Un único repositorio `VitSync-2.0` con:

```
apps/
  api/     Spring Boot (Maven)
  web/     React + Vite (pnpm)
docs/
  openapi.json    contrato generado, versionado en git
scripts/
```

El contrato `docs/openapi.json` se genera desde el backend en CI y **el build falla si el
fichero versionado no coincide con el generado**. El cliente TypeScript de `apps/web` se
deriva de ese mismo fichero.

La CI se organiza en jobs independientes (`api`, `web`) con filtros por ruta.

## Consecuencias

- `+` El drift front/back deja de ser un riesgo de proceso y pasa a ser un fallo de build.
- `+` La estructura ya está reflejada en el [README](../../README.md) y en la guía; no hay
  nada que reorganizar.
- `−` Hay que configurar filtros de ruta en GitHub Actions para que la CI no tarde el doble
  en cada PR. Mientras solo exista `apps/api`, el job `web` no existe.
- `−` Al desplegar habrá que indicar el directorio raíz de cada aplicación en Render y en
  Vercel. Es configuración de una sola vez.
- `−` **A vigilar**: si algún día el frontend o el backend se abren a colaboradores
  distintos con permisos distintos, el monorepo obliga a usar `CODEOWNERS` en vez de
  permisos de repositorio.
