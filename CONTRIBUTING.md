# Cómo se trabaja en VitSync 2.0

Este documento son las reglas del repositorio. No son preferencias: son lo que la CI
comprueba y lo que se revisa en cada PR.

> Contexto: [HANDOFF](docs/HANDOFF.md) · plan completo: [guía](docs/GUIA_CONSTRUCCION.md) ·
> alcance: [SCOPE](docs/SCOPE.md) · decisiones: [ADRs](docs/adr)

---

## 1. Requisitos de la máquina

| Herramienta | Versión | Para qué |
|---|---|---|
| JDK | 21 (LTS) | Backend |
| Docker | cualquiera reciente | `docker compose` y **Testcontainers** |
| Node + pnpm | Node 22 o superior, pnpm 9 (viene con `corepack`) | Frontend |

Maven no hace falta instalarlo: el repositorio trae el wrapper (`./mvnw`).

**Docker es obligatorio para ejecutar los tests**, no solo para levantar el entorno: los
tests de integración arrancan un PostgreSQL real con Testcontainers
([ADR-0008](docs/adr/0008-estrategia-de-pruebas.md)).

## 2. Arrancar el proyecto

```bash
docker compose up -d                 # Postgres 16 (vitsync y vitsync_dev) + MinIO + Mailpit
cd apps/api
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local,demo"
```

Comprobación: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

Servicios locales:

| Servicio | URL | Credenciales |
|---|---|---|
| API | http://localhost:8080 | — |
| PostgreSQL | `localhost:5433/vitsync` | `vitsync` / `vitsync` |
| MinIO (consola) | http://localhost:9001 | `minio` / `minio12345` |
| Mailpit (bandeja) | http://localhost:8025 | — |

### Usuarios de demostración (solo perfil `local`)

Hay dos bases de datos en el mismo contenedor de PostgreSQL:

| Base | Perfiles | Para qué |
|---|---|---|
| `vitsync_dev` | `local,demo` | Desarrollo diario: usuarios y catálogo de demostración |
| `vitsync` | `local` | Probar flujos desde cero (registro, verificación...) sobre una base vacía |

`vitsync_dev` la crea el servicio `db-init` del compose al hacer `docker compose up -d`.

```bash
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local,demo"   # make api
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"        # make api-empty
```

En PowerShell el `-D...` va entre comillas; si no, PowerShell parte el argumento.

Con el perfil `demo` se crean al arrancar, si no existen, tres cuentas activas con datos
sintéticos. Sin `demo` no se crea ninguna: ni en la base vacía ni en producción.

| Rol | Email | Contraseña |
|---|---|---|
| Administrador | `admin@vitsync.test` | `Demostracion-2026` |
| Médico | `medico@vitsync.test` | `Demostracion-2026` |
| Paciente | `paciente@vitsync.test` | `Demostracion-2026` |

```bash
curl -i -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"paciente@vitsync.test","password":"Demostracion-2026"}'
```

Además se crea el **catálogo de demostración**: 12 especialidades, 4 centros y un cuadro
médico de 15 profesionales (`nombre.apellido@vitsync.test`, misma contraseña) con su
especialidad y sus centros. Son los mismos datos que muestra el frontend simulado, así que
cambiar de `pnpm dev:mocks` a `pnpm dev` no cambia lo que se ve.

```bash
curl -s 'localhost:8080/api/v1/specialties?q=cardiologia'
```

Los emails (verificación, recuperación) se ven en Mailpit. Las claves JWT de `local` se
generan en memoria al arrancar: al reiniciar la API hay que volver a hacer login.

Esas credenciales son de desarrollo y son públicas a propósito (`.gitleaks.toml` solo hace
una excepción con la contraseña demo). **Ningún secreto real entra
en el repositorio**; gitleaks lo comprueba en CI. v1 llegó a tener credenciales en el
historial de git: no se repite.

### Por qué el 5433 y no el 5432

Porque un PostgreSQL instalado en la máquina ocupa el 5432, y el contenedor no puede
compartirlo. El síntoma es confuso: la conexión llega a la instancia local, no al
contenedor, y falla con `password authentication failed for user "vitsync"`.

Publicando el contenedor en el **5433** no hay que parar ningún servicio ni exportar nada:
`docker compose up -d` y `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` funcionan
tal cual. Dentro de la red de compose el puerto sigue siendo el 5432.

Si el 5433 también te estorba, los dos ficheros leen la misma variable:

```bash
export DB_PORT=5544            # PowerShell: $env:DB_PORT = "5544"
docker compose up -d
cd apps/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend

Con la API arrancada (`make api`, perfiles `local,demo`):

```bash
corepack pnpm install
corepack pnpm --filter web dev      # http://localhost:5173
```

Lo que ya tiene endpoint (identidad, `/me` y el catálogo: especialidades, centros y cuadro
médico) va al backend a través del proxy de Vite (`/api` → `localhost:8080`): mismo origen,
así que la cookie del refresh funciona sin CORS. El resto (huecos de la agenda, citas,
informes, mensajes y el área del paciente) lo simula MSW (`apps/web/src/test/mocks`) hasta
que tenga endpoint. Los códigos de un solo uso llegan a Mailpit (http://localhost:8025). Si la
API no está arrancada, las peticiones responden `503 BACKEND_UNAVAILABLE`.

Sin backend ni Docker, con todo simulado:

```bash
corepack pnpm --filter web dev:mocks  # http://localhost:5173
```

En ese modo, en la pantalla de acceso hay un botón para entrar con la cuenta de
demostración, y el código de verificación es siempre `123456`.

| `VITE_API_MOCKS` | Qué se simula | Cuándo |
|---|---|---|
| `missing` | Solo lo que el backend aún no tiene | `pnpm dev` (por defecto en desarrollo) |
| `all` | Todo | `pnpm dev:mocks` y tests |
| `none` | Nada | Build de producción (por defecto) |

### Tipos del contrato

Los tipos de lo que ya existe en la API no se escriben a mano: se generan de
`docs/openapi.json` en `apps/web/src/shared/api/schema.d.ts`, y `shared/api/types.ts` los
reexporta con nombres cortos. Cuando un PR cambia el contrato:

```bash
cd apps/api && ./mvnw verify -Dopenapi.update=true     # regenera docs/openapi.json
corepack pnpm --filter web api:types                    # regenera los tipos del frontend
```

La CI comprueba que `schema.d.ts` está al día. Si un campo cambia, el frontend deja de
compilar donde lo usa, y el mock afectado (`identityHandlers`, `catalogHandlers`) se ajusta en
el mismo PR: la API simulada tiene que comportarse como la real.

## 3. Idioma

| Qué | Idioma |
|---|---|
| Código, nombres de tablas y columnas, ramas, commits, ADRs | **Inglés** |
| Interfaz y textos de usuario | **Español** |
| Documentación del repositorio (`docs/`) | Español |

v1 mezclaba los dos y lo parcheaba con anotaciones de mapeo. Aquí la frontera es explícita.

## 4. Ramas y commits

**Trunk-based con ramas cortas.** `master` siempre desplegable y protegida.

```
master
feat/VIT-71-encryption-base
fix/VIT-64-refresh-rotation
chore/VIT-31-bump-spring
```

**Conventional Commits**, en inglés y en imperativo:

```
feat(appointments): add cancel endpoint
fix(auth): revoke token family on refresh reuse
chore(deps): bump testcontainers to 1.21.3
docs(adr): accept ADR-0007 realtime strategy
```

Ámbitos habituales: `auth`, `appointments`, `reports`, `catalog`, `gdpr`, `storage`,
`deps`, `ci`, `adr`.

Reglas de `master`: PR obligatorio, CI verde, historial lineal (**squash merge**), sin
force-push.

## 5. Estilo de código

**Java** — Google Java Format vía Spotless. **Todavía no está configurado**: el plugin
entra en la Fase 11 junto con el resto del pipeline de calidad, y hasta entonces
`./mvnw spotless:apply` / `spotless:check` no existen como objetivos del proyecto ni los
ejecuta la CI. El día que se añada el plugin, el primer commit será el reformateo completo,
aparte de cualquier cambio funcional.

**TypeScript** (desde la Fase 9) — ESLint + Prettier, `strict: true`, y nada de `any` sin un
comentario que justifique por qué.

Otras reglas que no pilla el formateador:

- **`@Data` de Lombok está prohibido en entidades JPA**: rompe `equals`/`hashCode` con
  colecciones perezosas. Fue el problema P4 de v1. Usa `@Getter` y setters explícitos.
- **La entidad nunca cruza el controlador.** Siempre DTO, mapeado con MapStruct (decisión D5).
- **`public_id` (UUID) es lo único que sale al exterior**; el `id` interno no aparece nunca en
  una URL ni en un JSON (decisión D6).

## 6. Migraciones de base de datos

- Flyway es la **única** fuente de verdad del esquema. `ddl-auto=validate` en **todos** los
  perfiles, incluido desarrollo.
- **Una migración no se edita nunca después de mergearse.** Se corrige con una nueva.
- Nombre: `V<n>__<descripcion_en_ingles>.sql`, en
  `apps/api/src/main/resources/db/migration/`.
- Los datos de demostración van en un perfil aparte, **nunca** en `db/migration`.
- Si la migración toca el esquema, el PR incluye el cambio de entidades correspondiente: si
  no, `validate` rompe el build, que es exactamente lo que queremos.

## 7. Definition of Done

Un PR no se mergea sin esto. Está también en la plantilla de PR.

- [ ] Tests unitarios **y** de integración de lo que se ha tocado, en verde.
- [ ] Migración Flyway si el cambio toca el esquema.
- [ ] Contrato OpenAPI actualizado (se genera solo si las anotaciones están bien).
- [ ] Sin `TODO` sin issue asociado.
- [ ] **Sin cambios "temporales"**. Fue el error visible de v1.
- [ ] README o ADR actualizado si cambia una decisión.
- [ ] Ningún dato clínico en logs, en URLs ni en `audit_logs.details`.
- [ ] `./mvnw verify` en verde en local antes de abrir el PR.

## 8. Cuándo escribir un ADR

Si la decisión es **discutible** y **cara de revertir**, se cierra con un ADR en
[`docs/adr/`](docs/adr), no con un mensaje de chat. Elegir entre dos librerías equivalentes
no lo es; cambiar el modelo de datos, el mecanismo de autenticación o dónde viven los
ficheros, sí.

Si es una duda todavía abierta, va a [`docs/DUDAS_Y_CAMBIOS.md`](docs/DUDAS_Y_CAMBIOS.md)
hasta que se resuelva.

## 9. Seguridad

- Ningún secreto en el repositorio, en ninguna rama, en ningún commit. Ni siquiera
  temporalmente: el historial de git no olvida.
- Las variables sensibles se declaran en [`.env.example`](.env.example) **sin valor** y se
  inyectan por entorno.
- Si encuentras una vulnerabilidad, no abras un issue público: anótala y trátala en privado.
