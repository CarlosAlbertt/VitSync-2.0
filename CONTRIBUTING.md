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
| Node + pnpm | Node 22, pnpm 9 | Frontend (a partir de la Fase 9) |

Maven no hace falta instalarlo: el repositorio trae el wrapper (`./mvnw`).

**Docker es obligatorio para ejecutar los tests**, no solo para levantar el entorno: los
tests de integración arrancan un PostgreSQL real con Testcontainers
([ADR-0008](docs/adr/0008-estrategia-de-pruebas.md)).

## 2. Arrancar el proyecto

```bash
docker compose up -d                 # Postgres 16 + MinIO + Mailpit
cd apps/api
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Comprobación: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

Servicios locales:

| Servicio | URL | Credenciales |
|---|---|---|
| API | http://localhost:8080 | — |
| PostgreSQL | `localhost:5432/vitsync` | `vitsync` / `vitsync` |
| MinIO (consola) | http://localhost:9001 | `minio` / `minio12345` |
| Mailpit (bandeja) | http://localhost:8025 | — |

Esas credenciales son de desarrollo y son públicas a propósito. **Ningún secreto real entra
en el repositorio**; gitleaks lo comprueba en CI. v1 llegó a tener credenciales en el
historial de git: no se repite.

### Si ya tienes un PostgreSQL en el 5432

El contenedor y tu PostgreSQL local no pueden compartir puerto, y el síntoma es confuso:
la conexión llega a tu instancia local y falla con
`password authentication failed for user "vitsync"`.

No hace falta parar tu servicio ni editar ficheros versionados: el puerto del host es una
variable.

```bash
export DB_PORT=5433            # PowerShell: $env:DB_PORT = "5433"
docker compose up -d
cd apps/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

`docker-compose.yml` y el perfil `local` leen ambos `DB_PORT`, con 5432 por defecto.

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
feat/VIT-12-appointment-booking
fix/VIT-31-refresh-rotation
chore/VIT-40-bump-spring
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

**Java** — Google Java Format vía Spotless:

```bash
./mvnw spotless:apply     # formatea
./mvnw spotless:check     # lo que ejecuta la CI
```

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
