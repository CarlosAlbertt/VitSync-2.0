# Dudas y Cambios

> Registro vivo de cambios estructurales y dudas abiertas del repositorio.
> Lo de arriba es lo más reciente. Cuando una duda se cierra, se marca resuelta
> (y si la decisión es discutible, se convierte en un ADR bajo [`adr/`](adr)).

---

## 2026-09-12 — Cierre del Sprint 0

### Contexto

Quedaban sin hacer los cuatro entregables del Sprint 0 que listaba el
[HANDOFF §5](HANDOFF.md): ADRs, entorno local, migración baseline y CI. Además había que
cerrar Q1 (versión de Spring Boot), que bloqueaba la redacción del ADR del stack.

### Decisión sobre Q1: se fija Spring Boot 4.1.1

Se descartó bajar a 3.5. El razonamiento completo está en el
[ADR-0001](adr/0001-stack-tecnologico.md); en corto: 3.5 es la última línea 3.x y está en
mantenimiento, así que arrancar ahí significa hacer la migración igual pero más tarde y con
código de producto encima. La guía vale por el diseño, no por los nombres de los artefactos
Maven, y esos se corrigen.

Documentación alineada con 4.1.1:

- Tabla de stack de la guía (Parte B) y nota nueva con la equivalencia de starters 3.x → 4.x.
- El bloque de springdoc de la Fase 3.2 ya no fija la versión 2.6.0: la línea compatible con
  Boot 4 se decidirá al entrar en esa fase.

### Cambios realizados

**1. Los ocho ADRs, escritos y aceptados**

[0001](adr/0001-stack-tecnologico.md) stack ·
[0002](adr/0002-monorepo-frente-a-dos-repositorios.md) monorepo ·
[0003](adr/0003-modelo-de-usuario-sin-herencia-jpa.md) modelo de usuario ·
[0004](adr/0004-autenticacion-jwt-rs256-con-refresh-rotativo.md) autenticación ·
[0005](adr/0005-cifrado-en-reposo-y-blind-index.md) cifrado ·
[0006](adr/0006-almacenamiento-de-ficheros-s3.md) ficheros ·
[0007](adr/0007-estrategia-de-tiempo-real.md) tiempo real ·
[0008](adr/0008-estrategia-de-pruebas.md) pruebas.

Cada uno recoge el contexto de v1 con su evidencia, las alternativas descartadas y las
consecuencias, incluidas las que hay que vigilar.

**2. `docker-compose.yml`**

Postgres 16 + MinIO + Mailpit, con `healthcheck` en los tres y un servicio `minio-init` que
crea el bucket `vitsync-dev` al arrancar, para que la API no tenga que crearlo en caliente.

Dos desviaciones respecto al ejemplo de la guía, por motivos concretos:

- **Imágenes de MinIO desde `quay.io`**: `minio/minio` y `minio/mc` ya no se pueden
  descargar de Docker Hub (`pull access denied`).
- **Puerto del host configurable**: `${DB_PORT:-5432}`. Ver Q8.

**3. `V1__baseline_schema.sql`**

El DDL de la Fase 1.4 de la guía, con las 19 tablas núcleo. **Reordenado**: la guía presenta
`doctor_profiles` antes que `specialties`, y la FK no resuelve; aquí los catálogos van
primero. Añadido `CREATE EXTENSION IF NOT EXISTS citext`, que el DDL da por hecho.

`diseases`, `disease_treatments` y `care_assignments` aparecen en el ERD pero no en el DDL de
la guía: quedan para migraciones posteriores, cuando entren sus funcionalidades.

**4. Configuración de la aplicación**

- `application.properties`: Flyway activo, `ddl-auto=validate` (decisión D3),
  `open-in-view=false`, Actuator con solo `health` expuesto. Las variables de base de datos
  **sin valor por defecto**: si falta una, la aplicación no arranca.
- `application-local.properties`: perfil que apunta al `docker-compose`.
- `.env.example` en la raíz con las variables del Anexo C, sin valores.

**5. Testcontainers, y con ello `./mvnw verify` en verde (cierra Q2)**

`VitSyncApplicationTests` levanta ahora `postgres:16-alpine` con `@ServiceConnection`. La
prueba parece trivial pero, con Flyway y `validate`, comprueba que las migraciones aplican
desde cero y que el mapeo de Hibernate concuerda con el esquema.

Dos cosas que costaron y conviene no volver a descubrir:

- Boot 4 **ya no gestiona la versión de Testcontainers**: hay que importar `testcontainers-bom`
  en el `pom.xml`.
- **Mínimo Testcontainers 1.21.4.** Las versiones anteriores negocian una versión de la API
  de Docker por debajo de la mínima que acepta Docker Engine 29 (1.44) y fallan con
  `Could not find a valid Docker environment`, que no dice nada del problema real (un HTTP
  400 en `/info`). La línea 2.x no sirve como salida rápida: reorganiza los artefactos
  (ya no existen `org.testcontainers:junit-jupiter` ni `:postgresql`) y es una migración
  aparte.

**6. CI mínima** — [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)

Job `api` (`./mvnw verify`, con Testcontainers sobre el Docker del runner) y job `secrets`
(gitleaks). **No** incluye todavía Spotless, dependency-check, JaCoCo ni el drift de OpenAPI:
esas herramientas aún no están configuradas en el proyecto, y una CI que falla por pasos que
no existen es una CI que se acaba ignorando. Entran en la Fase 11, según se configuren.

**7. Documentos de proceso**

- [`SCOPE.md`](SCOPE.md): MVP, v2.1 y fuera de alcance, con los escenarios Gherkin de las
  reglas de negocio que no son evidentes (reserva, cancelación, acceso a informes, rotación
  de refresh, supresión RGPD).
- [`CONTRIBUTING.md`](../CONTRIBUTING.md): requisitos, arranque, idioma, ramas, commits,
  estilo, reglas de migraciones y Definition of Done.
- `.github/pull_request_template.md` con la DoD como checklist.
- `Makefile` con los atajos de la Fase 2.3.

### Inventario de ficheros

Todo lo que cambió en esta sesión, y por qué.

#### Creados

| Fichero | Qué es |
|---|---|
| `docker-compose.yml` | Postgres 16 + MinIO + Mailpit + `minio-init`. Healthcheck en los tres servicios. |
| `apps/api/src/main/resources/db/migration/V1__baseline_schema.sql` | Esquema base: 19 tablas, índices y la extensión `citext`. |
| `apps/api/src/main/resources/application-local.properties` | Perfil `local`: apunta al compose, Mailpit y health con detalle. |
| `docs/SCOPE.md` | MVP / v2.1 / fuera, con escenarios Gherkin de las reglas no evidentes. |
| `CONTRIBUTING.md` | Requisitos, arranque, idioma, ramas, commits, estilo, migraciones y DoD. |
| `.env.example` | Variables del Anexo C, **sin valores**, con qué es cada una y su ADR. |
| `Makefile` | `up`, `down`, `clean`, `logs`, `api`, `test`, `migrate-info`. |
| `.github/workflows/ci.yml` | Jobs `api` (`mvnw verify`) y `secrets` (gitleaks). |
| `.github/pull_request_template.md` | La Definition of Done como checklist del PR. |
| `docs/adr/0001…0008` | Los ocho ADRs, todos en estado *Aceptado*. |

#### Modificados

| Fichero | Cambio | Motivo |
|---|---|---|
| `apps/api/pom.xml` | `dependencyManagement` con `testcontainers-bom` 1.21.4; dependencias de test `spring-boot-testcontainers`, `junit-jupiter`, `postgresql` | Boot 4 no gestiona la versión de Testcontainers, y hace falta para que `verify` tenga una BD real |
| `apps/api/src/main/resources/application.properties` | Flyway, `ddl-auto=validate`, `open-in-view=false`, Actuator solo `health`, variables de BD sin valor por defecto | Decisión D3 y "fallar rápido" del Anexo C |
| `apps/api/src/test/java/com/vitsync/VitSyncApplicationTests.java` | `@Testcontainers` + `PostgreSQLContainer` con `@ServiceConnection` | Cierra Q2: el test valida migraciones y mapeo contra PostgreSQL real |
| `docs/GUIA_CONSTRUCCION.md` | Fila "Backend" de la Parte B → 4.1.x; nota nueva con la equivalencia de starters 3.x → 4.x; springdoc sin versión fijada | Alinear la guía con la decisión sobre Q1 |
| `docs/HANDOFF.md` | Estado actual, decisiones pendientes y "siguiente paso concreto" reescritos hacia el Sprint 1 | El Sprint 0 ya está cerrado |
| `README.md` | Tabla de estado, bloque "Arrancar en local", árbol del repo y tabla de documentación | Lo que decía ya no era cierto |
| `docs/adr/README.md` | Tabla de ADRs con enlaces y estado; lista de candidatos futuros | Estaban todos como "Pendiente de redactar" |

#### Borrados

| Fichero | Motivo |
|---|---|
| `apps/api/src/main/resources/db/migration/.gitkeep` | Ya no hace falta: la carpeta tiene la migración V1 |

#### Sin tocar, y a propósito

- **`.gitignore`**: ya ignoraba `.env` y `.env.*` con excepción para `.env.example`. Correcto
  tal cual.
- **Tu PostgreSQL local**: ver Q8. Se hizo configurable el puerto en vez de parar tu servicio.
- **`docs/openapi.json`**: aparece en la Fase 3, no antes.

### Verificación

Ejecutado de verdad, no solo escrito:

| Comprobación | Resultado |
|---|---|
| `./mvnw verify` | **verde** — 1 test, Flyway aplica V1 sobre PostgreSQL 16.15 en contenedor |
| `docker compose up -d` | los tres servicios `healthy`, bucket `vitsync-dev` creado |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | arranca en 3,7 s |
| `GET /actuator/health` | `{"status":"UP"}`, con `db` y `mail` en UP |
| Esquema resultante | 20 tablas (19 del dominio + `flyway_schema_history`) |

**Con esto se cumple el criterio de cierre del Sprint 0** que fijaba el HANDOFF.

---

## 2026-09-10 — Encaje del esqueleto de Spring Initializr

### Contexto

Se descargó el proyecto de Spring Initializr y se copió dentro del repositorio como
`VitSync-2.0/VitSync-2.0/`, es decir, una carpeta anidada con el mismo nombre que el repo
y sin encajar en la estructura de monorepo prevista.

### Cambios realizados

**1. Monorepo: `VitSync-2.0/` → `apps/api/`**

La carpeta descargada se movió a `apps/api/`, que es la estructura que ya declaraban el
[README](../README.md) (bloque "Estructura prevista del repositorio") y la
[guía](GUIA_CONSTRUCCION.md) (`apps/api` + `apps/web`).

**2. Paquete Java `Project.VitSync_20` → `com.vitsync`**

Initializr derivó el paquete del `groupId` `Project` y de un `artifactId` con guiones y
puntos, produciendo `Project/VitSync_20` (mayúscula inicial y guion bajo, contra la
convención Java). La guía fija `com.vitsync` como raíz en la Fase 4.1, y el test de
ArchUnit apunta ahí: `@AnalyzeClasses(packages = "com.vitsync")`.

Renombrado también:

| Antes | Después |
|---|---|
| `Project/VitSync_20/Application.java` | `com/vitsync/VitSyncApplication.java` |
| `Project/VitSync_20/ApplicationTests.java` | `com/vitsync/VitSyncApplicationTests.java` |

**3. `pom.xml`**

- `groupId` → `com.vitsync`, `artifactId` → `vitsync-api`.
- `name` y `description` rellenados.
- Eliminados los bloques vacíos que Initializr deja como plantilla
  (`<url/>`, `<licenses><license/></licenses>`, `<developers>`, `<scm>`): rompen
  validaciones en release y son ruido.
- Dependencias y plugins intactos.

**4. `.gitignore` unificado**

El `.gitignore` anidado de Initializr duplicaba y contradecía al del raíz (ignoraba
`.idea` sin `/`, `build/`, etc.). Se borró y se plegó en el raíz lo que faltaba:
Eclipse/STS, NetBeans, `*.iml`, `build/`, `HELP.md`. Un solo `.gitignore` para todo el
monorepo.

El `.gitattributes` **sí** se queda en `apps/api/` porque sus rutas (`/mvnw`) son
relativas a ese directorio.

**5. Limpieza**

- Borrado `HELP.md` (boilerplate de Initializr).
- Borrados `src/main/resources/static/` y `templates/`: el backend es API-only, el front
  es React (decisión D2 del [HANDOFF](HANDOFF.md)).
- Añadido `src/main/resources/db/migration/.gitkeep` para que la carpeta de Flyway
  sobreviva a git.

**6. Documentación sincronizada con la realidad**

- El README decía "Spring Boot 3.5", pero Initializr generó **4.1.1** (de ahí los starters
  nuevos `spring-boot-starter-webmvc` y los `*-test` modulares). Actualizado.
- La sección "No hecho todavía" del HANDOFF afirmaba que no existía `apps/api`. Actualizada.

### Estructura resultante

```
VitSync-2.0/
├── apps/
│   └── api/           pom.xml, mvnw, src/{main,test}/java/com/vitsync/
├── docs/              GUIA_CONSTRUCCION, HANDOFF, JIRA_SETUP, DUDAS_Y_CAMBIOS, adr/
├── scripts/jira/
├── .gitignore
└── README.md
```

### Verificación

`./mvnw -DskipTests compile` desde `apps/api/` → **verde** (exit 0, Java 21.0.8).

---

## Dudas abiertas

| # | Duda | Estado | Notas |
|---|---|---|---|
| Q8 | **Colisión en el puerto 5432** | Mitigada | Esta máquina tiene un PostgreSQL instalado escuchando en el 5432 además del contenedor. El síntoma engaña: la conexión llega al PostgreSQL local y falla con `password authentication failed for user "vitsync"`. En vez de parar el servicio, el puerto del host es ahora la variable `DB_PORT` (5432 por defecto), que leen tanto `docker-compose.yml` como el perfil `local`. Documentado en [CONTRIBUTING](../CONTRIBUTING.md#si-ya-tienes-un-postgresql-en-el-5432). |
| Q3 | **`apps/web/` no existe** | Pendiente | Corresponde a la Fase 9. La estructura ya reserva el hueco, y el job `web` de la CI se añadirá con él. |
| Q5 | **IntelliJ apunta al módulo antiguo** | Acción manual | `.idea/` referencia la ruta anterior. Recargar abriendo `apps/api/pom.xml` como proyecto Maven (o *File → Invalidate Caches*). No afecta al repo: `.idea/` está ignorado. |
| Q7 | **Licencia** | Abierta | MIT previsto, sin decidir. Mientras no exista el fichero `LICENSE`, el repositorio es legalmente "todos los derechos reservados" aunque esté público. Es lo único del Sprint 0 que sigue sin cerrar. |
| Q9 | **Tablero de Jira sin montar** | Pendiente | [JIRA_SETUP.md](JIRA_SETUP.md) y `scripts/jira/backlog.json` están listos; falta ejecutarlo. Es una acción manual con credenciales, no automatizable desde aquí. |
| Q10 | **Protección de `master` sin configurar** | Pendiente | PR obligatorio, CI verde y squash merge. Se configura en GitHub una vez la CI haya corrido al menos una vez. |

## Dudas resueltas

| # | Duda | Resolución |
|---|---|---|
| Q1 | Spring Boot 4.1.1 vs 3.5 | **4.1.1**, con la documentación alineada — [ADR-0001](adr/0001-stack-tecnologico.md) · 2026-09-12 |
| Q2 | `./mvnw verify` no pasaba | **Resuelta**: Testcontainers 1.21.4 + `@ServiceConnection`; verify en verde · 2026-09-12 |
| Q4 | `docker-compose.yml` no existía | **Resuelta**: creado y verificado con los tres servicios `healthy` · 2026-09-12 |
| Q6 | Monorepo vs dos repos | **Monorepo** — [ADR-0002](adr/0002-monorepo-frente-a-dos-repositorios.md) · 2026-09-12 |
