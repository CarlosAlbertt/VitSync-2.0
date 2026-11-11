# Dudas y Cambios

> Registro vivo de cambios estructurales y dudas abiertas del repositorio.
> Lo de arriba es lo más reciente. Cuando una duda se cierra, se marca resuelta
> (y si la decisión es discutible, se convierte en un ADR bajo [`adr/`](adr)).

---

## 2026-11-11 — Pantallas del catálogo contra la API real y tipos generados (VIT-80, VIT-70)

### Cambios realizados

- **Catálogo real en `pnpm dev`**: especialidades, centros y cuadro médico los responde el
  backend. Sus mocks pasan a `catalogHandlers`, que solo se usan con `pnpm dev:mocks` y en los
  tests. Los huecos de la agenda (`/doctors/{id}/slots`) siguen simulados hasta que exista la
  agenda; funcionan con cualquier id de médico.
- **Tipos generados del contrato** (`openapi-typescript`): `shared/api/schema.d.ts` sale de
  `docs/openapi.json` (`pnpm --filter web api:types`), y `types.ts` reexporta lo que ya existe
  en la API. Lo que aún no tiene endpoint sigue escrito a mano en la parte de abajo. La CI
  comprueba que los tipos están al día. Al generarlos, el compilador encontró dos diferencias
  reales con la API: el teléfono de un centro puede ser `null` (la portada lo daba por hecho) y
  los mocks no traían `postalCode`.
- **Campos obligatorios en las respuestas** (backend): springdoc no marcaba ningún campo como
  `required`, y los tipos generados los habrían hecho todos opcionales. Un `OpenApiCustomizer`
  los marca en todo lo que cuelga de una respuesta 2xx (Jackson escribe siempre todos los
  campos; los vacíos, como `null`). No toca las peticiones, donde hay opcionales de verdad, ni
  `ProblemDetail`.
- **Catálogo público también en el contrato**: los tres endpoints llevan `@SecurityRequirements`
  vacío. Antes Swagger les ponía el candado de Bearer aunque no lo piden.
- **Caché (criterio de VIT-80)**: las listas del catálogo usan `staleTime: 0`. Al volver a una
  pantalla se pintan al instante desde la caché y se revalidan en segundo plano. Antes tenían 5
  minutos, y en ese tiempo no se revalidaban.
- **Búsqueda de especialidades** con *debounce* de 250 ms: contra la API real, cada tecla era
  una petición. El mock busca ahora sin tildes, como la API.
- **Reserva simulada con médicos reales**: la cita (aún simulada) busca al médico en la API real
  si no está en los datos simulados (`bypass` de MSW).
- **Proxy de Vite sin backend**: responde `503 BACKEND_UNAVAILABLE` con el formato de error de la
  API y lo que hay que hacer, en vez del 500 genérico, que parecía un fallo del backend.

### Pendiente anotado

- Los datos simulados que aún no tienen endpoint (equipo médico y mensajes del área del
  paciente) enlazan a médicos simulados (`doc-1`). Con el catálogo real, "Pedir cita" desde ahí
  empieza la reserva desde el primer paso. Se resuelve cuando existan esos endpoints.

### Verificación

Tipos, lint, 30 tests y build del frontend. `SpecialtiesPage.test.tsx` prueba el criterio de
la caché (y falla con la configuración anterior) y la búsqueda sin tildes. De punta a punta
contra `vitsync_dev`: cuadro médico filtrado por especialidad y por centro con datos reales, y
reserva simulada con un médico real (201).

---

## 2026-11-11 — Cuadro médico público (VIT-78)

### Contexto

Segunda aplicación de [ADR-0009](adr/0009-referencias-entre-modulos-por-id.md), y la más
exigente: el listado mezcla datos de `iam` (nombre, colegiado, biografía) y de `catalog`
(especialidad y centros), se filtra por las dos cosas y se pagina. Criterio de VIT-78: el
filtrado ocurre en SQL, no en memoria.

### Cambios realizados

- **`GET /api/v1/doctors`**: público, paginado, por apellidos. Filtros `specialty` (slug) y
  `facility` (`publicId`). Un filtro que no corresponde a nada devuelve una página vacía, no un
  error. Solo datos públicos: nombre, colegiado, biografía, especialidad y centros activos.
- **Cómo se reparte la consulta**: `catalog` resuelve sus filtros (slug → id, centro → ids de
  sus médicos); `iam` devuelve la página ya filtrada y ordenada con su `count`
  (`DoctorDirectory.listed`, con *Specifications* de Spring Data); `catalog` completa la página
  con sus especialidades y centros, una consulta para cada cosa. Son siempre las mismas
  consultas, tenga la página 2 médicos o 100.
- **Quién sale en el cuadro médico**: perfil activo, con especialidad, y cuenta activa y no
  borrada. Es el mismo criterio con el que se calcula `doctorCount` en las especialidades.
- **`@EntityGraph`** en la consulta paginada: el usuario de cada médico viene en la misma
  consulta. Sin él, leer los nombres haría una consulta por fila.

### Verificación

`DoctorsApiTest`: filtros por separado y combinados, orden, solo datos públicos, que filtrar va
antes de paginar (el total es el de los filtrados), quién queda fuera, centros cerrados,
filtros que no existen, validación y un test que **vigila el N+1**: una página de 2 médicos y
otra de 6 tienen que hacer el mismo número de consultas (estadísticas de Hibernate, activadas
solo en los tests). `CatalogDemoSeederTest` comprueba además el cuadro médico de demostración.

---

## 2026-11-11 — Base de datos de demostración y frontend contra la API real (VIT-81)

### Contexto

Se pidió desarrollar contra datos reales en una base propia en vez de contra la API simulada.
La base de datos ya iba en su contenedor (`vitsync-db`); lo que faltaba era separar los datos
de demostración y que el frontend en desarrollo hablara con el backend por defecto.

### Cambios realizados

- **Dos bases en el mismo contenedor**: `vitsync_dev` (datos de demostración) y `vitsync`
  (vacía, para probar flujos desde cero). `vitsync_dev` la crea un servicio de un solo uso del
  compose (`db-init`), idempotente; no un script de `/docker-entrypoint-initdb.d`, que solo se
  ejecuta con el volumen vacío y no habría llegado a los entornos que ya existían.
- **Perfil `demo`** (`application-demo.properties`): apunta a `vitsync_dev` y activa los dos
  seeders, que pasan de `@Profile("local")` a `@Profile("demo")`. Se arranca con
  `local,demo` (`make api`); `local` solo usa la base vacía (`make api-empty`). Es el "perfil
  demo" que pedía el criterio de VIT-81.
- **Frontend**: `pnpm dev` pasa a ir contra la API real (modo `missing`: MSW solo simula lo
  que aún no tiene endpoint). El modo con todo simulado queda como `pnpm dev:mocks`.
  Desaparece `dev:api`, que es ahora el comportamiento por defecto.

### A tener en cuenta

- Con un volumen de Docker anterior, la base `vitsync` conserva los usuarios demo que creó el
  Sprint 1. No molestan; para dejarla vacía de verdad: `docker compose down -v` (borra también
  `vitsync_dev`, que se vuelve a sembrar al arrancar la API con `demo`).
- El catálogo del frontend sigue simulado hasta VIT-80: los ids de los centros simulados
  (`fac-turia`) no coinciden con los reales (UUID), y el cuadro médico (VIT-78) aún no existe;
  mezclar las dos fuentes rompería los filtros. Se conecta todo a la vez.

---

## 2026-11-11 — Catálogo: especialidades, centros y datos de demostración (VIT-76, VIT-77, VIT-81)

### Contexto

Primer PR del Sprint 2 y primera aplicación de [ADR-0009](adr/0009-referencias-entre-modulos-por-id.md):
`catalog` necesita saber cuántos médicos tiene cada especialidad, y los médicos son de `iam`.

### Cambios realizados

- **`GET /api/v1/specialties`**: público, paginado (`page` desde 0, `size` hasta 100), solo
  activas, por nombre. Filtros `q` (texto libre, con o sin tildes: se compara también con el
  slug) y `kind`. Cada especialidad trae `doctorCount`, que `catalog` pide a `iam` para toda la
  página en una consulta (`DoctorDirectory.countListedBySpecialty`).
- **`GET /api/v1/facilities`**: público, paginado, solo activos, por ciudad y nombre.
- **`PageResponse`** (`common/web`): `{content, page: {number, size, totalElements, totalPages}}`,
  la forma que ya esperaba el frontend. Se escribe a mano porque la serialización de `PageImpl`
  de Spring Data no es un contrato estable.
- **Parámetros inválidos** (`@Min`, `@Max`, `@Size` en la URL): `400 VALIDATION_FAILED` con el
  parámetro que falla, igual que un cuerpo inválido. Antes habrían salido como
  `MALFORMED_REQUEST`.
- **Datos de demostración** (solo perfil `local`): `DemoUsersSeeder` crea además 15 médicos y
  `CatalogDemoSeeder` (después, con `@Order`) las especialidades, los centros y la especialidad y
  los centros de cada médico, incluido `medico@vitsync.test`. Los dos se enlazan por número de
  colegiado. Mismos textos que el frontend simulado.
- **`doctor_facilities`** pertenece a `catalog` (lo dice su `package-info`): se escribe con una
  consulta nativa idempotente, sin entidad, porque es una tabla de enlace sin datos propios.

### Dirección de la dependencia entre módulos

`catalog` depende de `iam` (por su capa `application`) y nunca al revés: `ArchitectureTest`
prohíbe los ciclos. Por eso la especialidad del médico la asigna `catalog` llamando a
`DoctorDirectory.assignSpecialty`, y no `iam` leyendo el catálogo. El cuadro médico
(`GET /api/v1/doctors`, VIT-78) seguirá la misma dirección.

### Pendiente anotado

- **Catálogo en producción**: el seed es solo de `local`. En producción el catálogo lo dará de
  alta el panel de administración (Sprint 6); para el despliegue de demostración (Sprint 8)
  habrá que decidir cómo se carga.
- **Búsqueda sin tildes**: se resuelve comparando también con el slug. Si el catálogo crece
  (enfermedades, VIT-79), valorar la extensión `unaccent` de PostgreSQL.

### Verificación

Tests nuevos: `SpecialtiesApiTest` (paginación, orden, filtros, tildes, comodines literales,
recuento de médicos, validación, escritura cerrada), `FacilitiesApiTest`, `SearchPatternsTest`,
`CatalogDemoSeederTest` (incluida la idempotencia) y `NoCatalogDemoOutsideLocalTest`.
`./mvnw verify` → **152 tests en verde**.

De paso se corrige un test intermitente que no es de este PR: `OutboxWorkerTest` forzaba el
último reintento con `next_attempt_at = now()`, que es la hora de PostgreSQL (en Docker),
mientras el worker compara con la de la JVM. Con el reloj del contenedor unos ms por delante
(pasa justo después de arrancar Docker Desktop) el mensaje aún no tocaba y el test fallaba.
Ahora se pone un minuto en el pasado.

---

## 2026-11-11 — Referencias entre módulos por id (VIT-110, ADR-0009)

### Contexto

Q13 seguía abierta y bloqueaba las slices de catálogo y citas: el ejemplo de la guía modela
la cita con `@ManyToOne` hacia los perfiles de `iam`, y `ArchitectureTest` no lo permite.

### Decisión

[ADR-0009](adr/0009-referencias-entre-modulos-por-id.md): entre módulos se referencia por id
(`patientId`, `doctorId`, `facilityId`) con la FK en la base de datos; los datos de otro
módulo se piden a su capa `application`, que devuelve *records*, y en los listados siempre
por lote. Dentro de un módulo, las relaciones JPA siguen siendo libres.

### Cambios realizados

- `docs/adr/0009-referencias-entre-modulos-por-id.md` y su fila en el índice.
- Guía, Fase 4.3 y 5.2: `Appointment` y `BookAppointmentService` usan ids y `DoctorDirectory`.
- Comentarios de `ArchitectureTest` y `DoctorProfile`: apuntan al ADR en vez de a Q13.

Sin cambios de comportamiento: la regla ya la imponía `ArchitectureTest`.

---

## 2026-11-11 — Portada nueva y paleta del modo oscuro (VIT-109)

### Contexto

La portada del PR anterior no convencía: mucho margen a los lados, una foto de archivo poco
creíble y un modo oscuro en negro verdoso con botones blancos. Se revisaron las portadas de
Doctoralia, Top Doctors, Sanitas y Kry (Quirónsalud, Zocdoc, One Medical, Hims y Doctolib
bloquean los navegadores automáticos) y se tomaron los patrones que se repiten.

### Cambios realizados

- **Portada** (`features/home`): cabecera en banda de color de marca con el buscador y lo más
  buscado; cifras del catálogo montadas sobre la banda; servicios; especialidades; área del
  paciente con una captura real (`public/portal-preview.webp`); especialistas con su próxima
  hora; cómo pedir cita; centros; bloque para profesionales. Secciones con fondos alternos.
- **Sin foto de archivo**: se retira `hero.webp`. En su lugar, fichas reales de la aplicación
  (próximo hueco libre, videoconsulta y un resultado con su gráfica). El lateral de la pantalla
  de entrar pasa a ser una banda de color.
- **Pie** a todo el ancho en el color de marca, con columnas de enlaces y atención al paciente.
- **Ancho**: las páginas públicas usan un contenedor de 1440 px.
- **Paleta**: `--brand` pasa a teal profundo (#0c4a47) en claro; afecta también al banner de
  la próxima cita del área del paciente. El modo oscuro cambia a azul pizarra con el teal del
  logo como color de acción.
- **Recarga forzada**: con Ctrl+F5 la página se carga sin el service worker de MSW y las
  pantallas simuladas fallaban contra el backend. Si arranca sin el worker al mando, se recarga
  una vez.

### Verificación

Tipos, lint, 28 tests y build. Capturas de la portada a 1440 px, 1920 px y 375 px (sin scroll
horizontal), en claro y oscuro, y del área del paciente y de la pantalla de entrar en los dos
modos.

---

## 2026-11-05 — Autenticación del frontend contra la API real (VIT-108)

### Contexto

El PR del frontend (#11) dejó anotadas las diferencias entre su API simulada y la real. Este PR
las corrige y conecta la identidad (registro, verificación, acceso, sesión, recuperación y Mis
datos) con el backend. Rama `feat/VIT-108-web-real-auth`.

### Cambios realizados

- **Modo API**: `pnpm dev:api` (`vite --mode api`). Un proxy de Vite manda `/api` a
  `localhost:8080`, así el navegador ve un único origen: la cookie del refresh (`SameSite=Lax`,
  `Path=/api/v1/auth`) funciona sin CORS ni `SameSite=None`.
- **`VITE_API_MOCKS`** pasa de `true/false` a `all`, `missing` y `none`. En `missing`, MSW solo
  responde lo que el backend aún no tiene y deja pasar `/auth` y `/me`. Los datos simulados
  aceptan el JWT real y devuelven los de la paciente de demostración.
- **Contrato alineado**: `Me` con la forma de la API (`id`, `role`, `locale`, `emailVerified`,
  sin datos clínicos); el registro envía el NIF normalizado y no el consentimiento;
  `newPassword` en la recuperación; mensajes para `INVALID_OR_EXPIRED_CODE`,
  `TOO_MANY_ATTEMPTS`, `ACCOUNT_DISABLED` e `INVALID_REFRESH_TOKEN`.
- **Contraseña**: la misma regla que el backend (12 caracteres o más, máximo 72 bytes), sin
  exigir mayúscula, número ni símbolo.
- **API simulada**: reproduce la real en identidad, incluidos el 409 sin revelar campo, el
  código quemado tras 10 o 5 fallos y el cierre de sesiones al cambiar la contraseña.
- **Mis datos**: nombre, apellidos, teléfono e idioma se guardan con `PATCH /api/v1/me`.
  Identificación y datos de salud se muestran con la etiqueta «Ejemplo»: la API aún no los
  devuelve.
- **Tests**: 28 (antes 24). Nuevos: cuerpo exacto del registro y recuperación con `newPassword`
  y con el código quemado.

### Verificación de punta a punta

Con `docker compose up -d`, el backend en perfil `local` y `pnpm dev:api`, un navegador
automatizado recorrió: registro (201), código desde Mailpit, verificación (204), acceso (200),
recarga con la sesión recuperada por la cookie (refresh 200), `PATCH /me` (200), cierre de sesión
(204, después el panel pide entrar), recuperación con el código de Mailpit (202 y 204), error
genérico con la contraseña antigua (401) y acceso con la nueva.

### Deuda consciente

- **Datos clínicos del paciente** (fecha de nacimiento, NIF, grupo sanguíneo, alergias,
  contacto de emergencia): sin endpoint. Mis datos, Historial y el resumen de emergencia los
  muestran de ejemplo (`shared/demo/patient.ts`).
- **Roles**: médico y administrador entran al mismo área del paciente; las vistas por rol
  llegarán con sus funcionalidades.
- Los tipos siguen escritos a mano; generarlos de `docs/openapi.json` (D10) queda pendiente.

---

## 2026-11-05 — Frontend React con API simulada (VIT-23)

### Contexto

Primer PR del frontend (`apps/web`), rama `feat/VIT-23-web-foundation`. Se adelanta parte
del Sprint 2: la aplicación entera funciona en el navegador contra una **API simulada con
MSW**, sin hablar todavía con el backend. Conectarla es el PR siguiente.

### Cambios realizados

- **`apps/web`**: React 19 + TypeScript + Vite. Web pública (portada, cuadro médico,
  especialidades, contacto), autenticación completa (entrar, registro, verificación,
  recuperación), reserva de cita y área del paciente. 24 tests (Vitest + Testing Library).
- **Cliente HTTP** (`shared/api/client.ts`): access token solo en memoria, cookie del refresh
  con `credentials: "include"` y refresh *single-flight* (una sola llamada a `/auth/refresh`
  aunque fallen varias peticiones a la vez), como pide el ADR-0004.
- **Workspace de pnpm** en la raíz (`package.json`, `pnpm-workspace.yaml`, `pnpm-lock.yaml`).
- **CI**: job `web` (lint, tipos, tests y build). Objetivo `web` en el `Makefile`,
  `VITE_API_MOCKS` en `.env.example`, y sección de frontend en README y CONTRIBUTING.

### Diferencias de contrato entre el mock y la API real (`master`)

Revisadas al abrir el PR. Se corrigen en el PR de conexión, no en este:

| Punto | Mock | API real | Cómo se resuelve |
|---|---|---|---|
| Registro | Sin `nif` | `nif` obligatorio (NIF/NIE con letra) | Añadir el campo al formulario |
| Recuperación | `password` | `newPassword` | Renombrar en el cliente |
| Código incorrecto | `INVALID_CODE` | `INVALID_OR_EXPIRED_CODE` | Usar el código de la API |
| `Me` | `publicId`, `birthDate`, `bloodType`, `allergies`, `emergencyContact` | `id`, `locale`, `emailVerified`; sin datos clínicos | Alinear `Me`; los datos clínicos irán en un endpoint del perfil del paciente |
| Errores sin mock | — | `TOO_MANY_ATTEMPTS`, `ACCOUNT_DISABLED`, `INVALID_REFRESH_TOKEN` | Mensajes en la UI |
| Catálogo, citas, informes, etc. | Simulados | **No existen** todavía (Sprint 2 y siguientes) | Esas pantallas siguen con el mock hasta que exista su endpoint |

### Pendiente

- Conectar los flujos de autenticación a la API real (`VITE_API_MOCKS=false`) con un proxy de
  Vite de `/api` a `localhost:8080`: mismo origen, así la cookie `HttpOnly` funciona sin
  `SameSite=None` ni CORS.
- Añadir el check `web` a los obligatorios del ruleset de `master`.

---

## 2026-11-04 — Recuperación de contraseña por código (VIT-65)

### Contexto

Paso 6 y último del plan del Sprint 1. Rama `feat/VIT-65-password-reset`, apilada sobre
`feat/VIT-64-refresh-rotation`. Reutiliza la tabla de códigos de V2 (`purpose =
PASSWORD_RESET`) y el outbox: no hay migración.

### Cambios realizados

| Endpoint | Respuesta |
|---|---|
| `POST /api/v1/auth/password-reset/request` `{ email }` | **`202` vacío siempre**, exista o no la cuenta |
| `POST /api/v1/auth/password-reset` `{ email, code, newPassword }` | `204`; `400 INVALID_OR_EXPIRED_CODE` / `PASSWORD_BREACHED`; `429 TOO_MANY_ATTEMPTS` |

- Solo las cuentas **activas** reciben código (una sin verificar, no: el camino para ella es
  el login, que reenvía el de verificación).
- Código de 6 dígitos, **15 minutos**, **5 intentos**, de un solo uso, guardado con HMAC.
- **Como mucho un email por minuto** aunque se pida varias veces: protege el buzón de quien
  quiera inundarlo con peticiones a nombre de otro.
- Con el código correcto: contraseña nueva (misma política y HaveIBeenPwned que el registro),
  **todas las sesiones revocadas** (todas las familias de refresh) y `PASSWORD_RESET` en
  `audit_logs`.
- Una contraseña filtrada se rechaza **antes** de mirar el código: no gasta un intento.

### Decisiones

| Decisión | Motivo |
|---|---|
| El hash BCrypt de la contraseña nueva se calcula antes de comprobar el código | El tiempo de respuesta no depende de si el código es válido, y la transacción no espera a BCrypt |
| Los access tokens ya emitidos no se invalidan | Son sin estado (ADR-0004): caducan solos en 15 minutos. Revocarlos exigiría consultar la BD en cada petición |

### Riesgo conocido

`password-reset/request` hace una escritura más cuando la cuenta existe (código + outbox), así
que su tiempo de respuesta difiere en unos milisegundos. Medirlo exige muchas peticiones; lo
que lo corta es el rate limiting por IP (Bucket4j, guía 6.4), pendiente para un sprint
posterior. La alternativa (encolar siempre y resolver la cuenta en el worker) obligaría a
`notification` a depender de `iam`, que ArchitectureTest prohíbe con razón.

### Tests

`PasswordResetApiTest` (7, Testcontainers): respuesta idéntica con y sin cuenta, cambio de
contraseña con cierre de **todas** las sesiones y entrada de auditoría (la vieja deja de
valer, la nueva entra), código de un solo uso, 5 fallos → 429, contraseña filtrada sin gastar
el código, un solo email por minuto, y ninguno para cuentas sin verificar.

---

## 2026-11-04 — Refresh rotativo con detección de reuso (VIT-64)

### Contexto

Paso 5 del plan del Sprint 1: el hueco concreto de v1 según el ADR-0004. Rama
`feat/VIT-64-refresh-rotation`, apilada sobre `feat/VIT-63-login-jwt`.

### Cambios realizados

**`POST /api/v1/auth/refresh`** (solo con la cookie) → `200 { accessToken, user }` + cookie
nueva, o `401 INVALID_REFRESH_TOKEN` y la cookie borrada.

| Caso | Reacción |
|---|---|
| Token vigente | Se marca canjeado (`revoked_at` + `last_used_at`) y se emite otro de la **misma familia** |
| Token **ya canjeado** (reuso) | **Se revoca la familia entera** y se registra `REFRESH_TOKEN_REUSE` en `audit_logs` |
| Token de una sesión cerrada (logout) | 401, **sin** alerta: no se canjeó nunca, no es un robo |
| Caducado, desconocido o ausente | 401 |
| Cuenta suspendida o borrada | 401 y se revoca la familia |

- **`SELECT … FOR UPDATE`** sobre la fila del token: dos canjes simultáneos se ponen en fila;
  el primero rota y el segundo lo ve canjeado (reuso). Probado con 5 peticiones a la vez:
  exactamente un `200`.
- La transacción se confirma **antes** de lanzar el 401, para que la revocación de la familia
  y la entrada de auditoría no se deshagan.
- El 401 es el mismo en todos los casos: decir "reutilizado" avisaría al atacante de que se le
  ha detectado.

**`common/audit`**: `AuditLog` (append-only, sin FK al usuario como pide V1) y `AuditTrail`
(`Propagation.MANDATORY`, copia el `traceId` del MDC). `details` solo lleva hechos
(`{"reaction":"FAMILY_REVOKED"}`), nunca datos personales.

### Decisiones

| Decisión | Motivo |
|---|---|
| Reuso = token **canjeado** presentado otra vez, no "cualquier token revocado" | Si no, cada logout seguido de un reintento del cliente sería una falsa alarma en `audit_logs` |
| Bloqueo pesimista en vez de optimista | Con `@Version`, el segundo canje fallaría con una excepción genérica; con el bloqueo, ve el estado real y aplica la regla de reuso |
| Mismo 401 para todo | El cliente solo necesita saber que debe volver a hacer login |

### Tests

`RefreshRotationApiTest` (6, Testcontainers): los dos escenarios Gherkin de SCOPE ("renovación
normal" y "reuso de un refresh revocado", con su entrada en `audit_logs`), sesión cerrada sin
alerta, token ausente/desconocido/caducado, cuenta suspendida y **concurrencia** (5 canjes
simultáneos → un solo `200`). `RefreshTokenTest` (3): vida de 7 días, canjeado frente a
revocado, recorte del `User-Agent`.

### Pendiente

- **Limpieza de `refresh_tokens` caducados** ("a vigilar" del ADR-0004). Solo se pueden borrar
  los ya caducados: un token revocado pero vigente tiene que seguir en la tabla para detectar
  su reuso.
- El cliente tiene que **serializar los refresh** (una sola promesa compartida): si no, dos
  pestañas que refrescan a la vez cierran la sesión (ADR-0004). Es la historia "Cliente HTTP
  tipado con refresh single-flight" del Sprint 2.

---

## 2026-11-04 — Login con JWT RS256, `/me` y usuarios demo (VIT-63 + VIT-67)

### Contexto

Paso 4 del plan del Sprint 1. Rama `feat/VIT-63-login-jwt`, apilada sobre
`feat/VIT-66-outbox-email-verification`.

### Cambios realizados

**Endpoints**

| Método y ruta | Respuesta |
|---|---|
| `POST /api/v1/auth/login` `{ email, password }` | `200 { accessToken, user }` + cookie `vitsync_refresh` |
| `POST /api/v1/auth/logout` | `204` siempre; revoca la familia del refresh y borra la cookie |
| `GET /api/v1/me` | `200 Me` (Bearer) |
| `PATCH /api/v1/me` `{ firstName?, lastName?, phone?, locale? }` | `200 Me`; ausente = sin cambios, `phone: ""` lo borra |

`Me = { id, email, role, firstName, lastName, phone, locale, emailVerified }`, con `id` =
`public_id`. Sin NIF: no hace falta fuera del alta.

**JWT (`common/security`)**

- `AccessTokenService`: RS256, 15 minutos, claims mínimos (`sub` = `public_id`, `role`,
  `jti`, `iss`, `iat`, `exp`). Nada personal: un JWT se lee con base64.
- `JwtConfig`: `NimbusJwtEncoder` para firmar y `NimbusJwtDecoder` que **solo acepta RS256**
  (cierra el cambio de `alg` a `none`/HS256), valida emisor y caducidad.
- `JwtKeys`: carga el par RSA en PEM (`JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, admite `\n`
  literales) y lo valida al arrancar: RSA, 2048+ bits y pública que corresponde a la privada.
  **Sin claves la app no arranca**, salvo con `allow-ephemeral-keys=true` (`local` y tests),
  que genera un par en memoria. No hay ningún PEM en el repositorio.
- `SecurityConfig`: `oauth2ResourceServer().jwt()` con el conversor `role` → `ROLE_<role>`, y
  el mismo entry point que el resto, así un token caducado o manipulado da el 401 RFC 9457.

**Login (`SessionService`)**

- **Respuesta y tiempo uniformes**: si el email no existe se compara contra un hash ficticio
  del mismo coste. Email desconocido y contraseña incorrecta dan el mismo `401
  INVALID_CREDENTIALS`.
- Solo **después** de acertar la contraseña: `403 ACCOUNT_NOT_VERIFIED` (y un código nuevo
  por email si el anterior ya no sirve) o `403 ACCOUNT_DISABLED`.
- Refresh opaco de 32 bytes (`SecureRandom`, base64url), **solo su SHA-256 en la BD**,
  `family_id` nuevo por login, 7 días.
- Cookie `HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth`. `Secure` se apaga solo en
  `local` (sin HTTPS).

**Usuarios demo (VIT-67)**: `DemoUsersSeeder` con `@Profile("local")` crea, si no existen,
`admin@`, `medico@` y `paciente@vitsync.test` (activos, con su perfil; el médico con su
`doctor_profiles`), contraseña `Demostracion-2026`, documentada en CONTRIBUTING. El paciente
coincide con el del mock del frontend. Fuera de `local` el bean no existe.

### Decisiones

| Decisión | Motivo |
|---|---|
| **Resource server de Spring Security** en vez de un filtro JWT propio | Es el filtro Bearer estándar: validación de firma, `alg`, emisor y caducidad ya probada. Un filtro casero es justo el sitio donde se cuelan los fallos |
| Claves efímeras en `local` y tests | Un PEM privado en el repo haría saltar a gitleaks (con razón). Coste: en local, reiniciar la API obliga a hacer login de nuevo |
| El login reenvía el código de verificación si ya no sirve | Resuelve el "pendiente" del PR anterior sin un endpoint de reenvío que permitiría enumerar emails: hace falta la contraseña |
| `/auth/logout` público y solo con la cookie | Se puede cerrar sesión aunque el access token haya caducado |
| `.gitleaks.toml` con una única excepción: la contraseña demo | Es pública a propósito; cualquier otro secreto sigue rompiendo la CI |
| `DoctorProfile` entra ya | El médico demo necesita su fila: un `DOCTOR` sin perfil es el fallo de v1 (ADR-0003) |
| `refresh_tokens.ip_address` (`inet`) sin mapear | No se usa todavía; Hibernate solo valida lo mapeado |

### Comprobado a mano (perfil `local`, Postgres y Mailpit reales)

Login del paciente demo → `200` con cookie; `GET /me` → `200`; registro → email en Mailpit
con el código → `verify` → `204` → login → `200`. Sin variables de entorno, la app no arranca.

### Tests

| Test | Qué protege |
|---|---|
| `JwtKeysTest` (4) | PEM con `\n` literales, claves ausentes, pública que no corresponde, claves cortas o basura sin eco |
| `AccessTokenServiceTest` (4) | Claims mínimos y 15 min, firma de otra clave, `alg: none`, token caducado |
| `LoginApiTest` (9, Testcontainers) | Cookie con sus atributos y hash en BD, 401 idéntico, email sin distinguir mayúsculas, no verificado (con y sin reenvío), contraseña incorrecta en cuenta no verificada, suspendida, `/me` con token válido/ausente/manipulado, `PATCH /me` parcial con teléfono cifrado, logout |
| `DemoUsersSeederTest` (3) y `NoDemoUsersOutsideLocalTest` (1) | Tres usuarios activos con perfil, idempotencia, login del paciente demo, sin seeder fuera de `local` |

### Pendiente

- `POST /api/v1/auth/refresh` con rotación y detección de reuso: PR siguiente (VIT-64).
- Readiness: Boot marca `health` como `UP` antes de que el seeder termine (corre después del
  arranque). En local solo importa si se hace login en el primer segundo.

---

## 2026-11-04 — Outbox de emails y verificación por código (VIT-66 + VIT-62)

### Contexto

Paso 3 del plan del Sprint 1. Rama `feat/VIT-66-outbox-email-verification`, apilada sobre
`feat/VIT-61-patient-registration`.

### Cambios realizados

**Migración `V2__one_time_codes.sql`**

- Tabla `one_time_codes` (verificación y recuperación, distinguidos por `purpose`): hash del
  código, caducidad, intentos y `consumed_at`. Índice único parcial: **como mucho un código
  vivo por usuario y propósito**, garantizado por la BD.
- `CHECK` en `outbox_messages.status` (`PENDING`, `SENT`, `FAILED`), que V1 no tenía. Se
  añade con un `ALTER` en V2: V1 no se toca.

**Outbox (módulo `notification`)**

| Pieza | Qué hace |
|---|---|
| `EmailOutbox.enqueue` | Inserta el mensaje con `Propagation.MANDATORY`: **exige** la transacción de quien llama, así que el email queda ligado al cambio que lo provoca |
| Payload | `{"data": "v1:…"}`: destinatario y variables (con el código vigente) **cifrados** con `FieldEncryptor` |
| `OutboxWorker` | Reserva con `FOR UPDATE SKIP LOCKED`, envía y marca, **un mensaje por transacción**. Backoff 30 s × 2ⁿ con tope de 1 h; `FAILED` tras 8 intentos. Entrega *al menos una vez* |
| `OutboxScheduler` | `@Scheduled(fixedDelay)` cada 5 s (1 s en `local`). Se apaga con `vitsync.outbox.worker.enabled=false` (tests) |
| `SmtpEmailSender` | `JavaMailSender`; en `local`, Mailpit (http://localhost:8025) |
| `EmailTemplate` | Plantillas de texto plano en español: `VERIFY_EMAIL` y `PASSWORD_RESET` |

`last_error` guarda **solo el tipo de excepción**: el mensaje de un error SMTP suele citar el
destinatario.

**Verificación (módulo `iam`)**

- El registro emite el código y encola el email **en su misma transacción**.
- `POST /api/v1/auth/verify` `{ email, code }` → `204`; la cuenta pasa a `ACTIVE`.
- Código de 6 dígitos con `SecureRandom`, vigente 10 minutos, de un solo uso.
- Guardado como **HMAC con el pepper del blind index** y separación de dominio
  (`otp:<propósito>:<usuario>:<código>`); comparación en tiempo constante.
- **10 intentos fallidos → `429 TOO_MANY_ATTEMPTS`**, y desde ahí ni el código correcto sirve.
  La transacción se confirma **antes** de lanzar el error, para que el intento quede contado.
- Email inexistente, cuenta ya activa, código incorrecto, caducado o usado: **la misma
  respuesta** (`400 INVALID_OR_EXPIRED_CODE`).

**Transversal**: `Clock` como bean (`ClockConfig`) para las reglas con tiempo.

### Decisiones

| Decisión | Motivo |
|---|---|
| Una tabla de códigos para verificación y recuperación | Misma forma y mismas reglas; el PR de recuperación ya no necesita migración |
| Hash del código con el pepper del blind index | Un SHA-256 de 6 dígitos se revierte al instante con la BD robada. Reutilizar el pepper con separación de dominio evita un secreto más que custodiar |
| Payload del outbox cifrado | Contiene un código vigente y el email: ilegible en un volcado, como el resto de datos personales |
| SMTP genérico (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`) en vez de `MAIL_PROVIDER_API_KEY` | El proveedor se decide en el Sprint 8; casi todos aceptan SMTP. `.env.example` actualizado |
| Límite de intentos por código, no por IP | Cumple el criterio de VIT-62 (429 tras 10 fallos). El rate limiting por IP (Bucket4j, guía 6.4) sigue pendiente |

### Tests

| Test | Qué protege |
|---|---|
| `OneTimeCodeTest` (4) | Un solo uso, caducidad, conteo de intentos, código agotado rechazado aunque sea correcto |
| `OutboxBackoffTest` (1) | Backoff exponencial con tope |
| `OutboxWorkerTest` (4, Testcontainers) | `MANDATORY` sin transacción falla, payload cifrado, envío → `SENT`, fallo → reintento programado sin datos en `last_error`, último intento → `FAILED` |
| `EmailVerificationApiTest` (5, Testcontainers) | Registro → email → verificación (el código se saca del email enviado), un solo uso, 10 fallos → 429, caducado, email desconocido, código guardado con hash |

### Pendiente

- **Reenvío del código**: si caduca o se agota, hoy no hay forma de pedir otro. Se resuelve en
  el PR del login: un login con la contraseña correcta de una cuenta sin verificar emite un
  código nuevo (sin endpoint extra y sin permitir enumeración).

---

## 2026-11-04 — Registro de pacientes (VIT-61)

### Contexto

Paso 2 del plan del Sprint 1. Rama `feat/VIT-61-patient-registration`, apilada sobre
`feat/VIT-71-encryption-base` (usa `FieldEncryptor` y `BlindIndex`).

### Cambios realizados

**Endpoint**: `POST /api/v1/auth/register` → `201 { email, status: "PENDING_VERIFICATION" }`.
Público (abierto solo ese endpoint y solo con `POST` en `SecurityConfig`).

**Módulo `iam`**, primeras clases en cada capa:

| Capa | Clases |
|---|---|
| `domain` | `User` y `PatientProfile` (sin herencia, `@MapsId`, ADR-0003), `Role`, `UserStatus`, `Nif`, `PasswordPolicy`, `Emails` |
| `application` | `RegistrationService`, `RegisterPatientCommand`, `RegisteredPatient` |
| `infrastructure` | `UserRepository`, `PatientProfileRepository`, `BreachedPasswordChecker` + `PwnedPasswordsClient` |
| `api` | `AuthController`, `RegisterRequest`, `RegistrationResponse`, `AuthMapper` (MapStruct), `@ValidNif`, `@ValidPassword` |

**Reglas que implementa:**

- **NIF/NIE** con letra de control (algoritmo oficial, módulo 23; X/Y/Z → 0/1/2). Se
  normaliza (sin espacios ni guiones, mayúsculas) antes de cifrarlo y de calcular `nif_hash`.
- **Contraseña**: 12+ caracteres y **72 bytes como máximo** (límite de BCrypt, contado en
  UTF-8). Sin reglas de composición, como recomienda NIST SP 800-63B.
- **HaveIBeenPwned** por k-anonimato (solo viajan 5 caracteres del SHA-1, con
  `Add-Padding`). Timeout de 2 s y **fallo abierto**: si no responde, aviso en el log y se
  continúa. En los tests está apagado (`vitsync.hibp.enabled=false`) o simulado.
- **409 `ACCOUNT_ALREADY_EXISTS`** idéntico para email y NIF duplicados. Comprobación
  previa + índice único como garantía real ante registros simultáneos (se traduce el
  `23505` de PostgreSQL; cualquier otra violación sigue siendo un 500).
- `User` y `PatientProfile` en **una sola transacción** (`TransactionTemplate`). La llamada a
  HaveIBeenPwned y el hash BCrypt van **fuera**, para no tener una conexión de la BD ocupada
  mientras se espera a la red.

### Decisiones

| Decisión | Motivo |
|---|---|
| **BCrypt coste 12**, no Argon2id | Lo fija el ADR-0004; Argon2 en Spring Security exige BouncyCastle. Envuelto en `DelegatingPasswordEncoder` (`{bcrypt}` delante del hash): migrar a Argon2id más adelante no obliga a nadie a cambiar la contraseña |
| Coste 4 **solo en tests** (`vitsync.security.bcrypt-cost`) | Con 12, cada registro de un test cuesta ~250 ms sin probar nada más |
| **MapStruct 1.6.3** entra ahora | Decisión D5. Con `lombok-mapstruct-binding` para que vea los getters de Lombok |
| Email normalizado en la aplicación aunque la columna sea `citext` | El driver envía `varchar`, y `citext = varchar` compara como texto normal: sin normalizar, `Ana@x.es` y `ana@x.es` podrían no coincidir en una búsqueda |
| Tests de integración con **contenedor singleton** (`support/IntegrationTest`) | Mitigación que pide el ADR-0008: un PostgreSQL para toda la suite, no uno por clase. Los tests usan datos aleatorios en vez de limpiar la BD |
| Sin consentimiento RGPD en el registro | Es la historia "Registro de consentimientos versionado" (épica RGPD); no está en el Sprint 1 |

### Tests

| Test | Qué protege |
|---|---|
| `NifTest` (20) | DNI y NIE válidos, letra incorrecta, formatos inválidos, normalización |
| `PasswordPolicyTest` (3) | Mínimo de 12, máximo de 72 bytes en UTF-8, emojis como un carácter |
| `PwnedPasswordsClientTest` (5) | Solo viaja el prefijo, relleno con recuento 0, fallo abierto ante 5xx y ante timeout |
| `RegistrationApiTest` (6, Testcontainers) | Alta con perfil, **NIF ilegible en la BD** (`v1:…`, hash = blind index) y legible por la entidad, 409 idéntico por email y por NIF, NIF inválido, contraseña corta y filtrada |

### Pendiente

- La verificación de email llega en el PR siguiente: hasta entonces la cuenta se queda en
  `PENDING_VERIFICATION`.
- Limitación de peticiones por IP (Bucket4j, guía 6.4): no está en el alcance del sprint.

---

## 2026-11-04 — Cifrado base en `common/crypto` (VIT-71 + VIT-72)

### Contexto

Primer PR del Sprint 1 (paso 1 del plan): el registro va a guardar el NIF cifrado y su
blind index, así que el cifrado tiene que existir antes. Rama `feat/VIT-71-encryption-base`.
Implementa [ADR-0005](adr/0005-cifrado-en-reposo-y-blind-index.md) sin cambiar ninguna
decisión.

### Cambios realizados

Paquete `com.vitsync.common.crypto`:

| Clase | Qué hace |
|---|---|
| `FieldEncryptor` | AES-256-GCM, IV aleatorio de 12 bytes, tag de 128 bits. Guarda `v<n>:` + base64(iv‖cifrado). Descifra cualquier versión conocida; `needsRotation(stored)` dice si hay que recifrar |
| `KeyRing` | Clave actual + anteriores. Valida al arrancar: 32 bytes en base64, versión `v\d+`, sin versiones duplicadas, y rechaza la clave a cero salvo con `allow-insecure-dev-key=true` |
| `BlindIndex` | HMAC-SHA256 en hex (64 caracteres) del valor con `trim` y mayúsculas. Pepper de 32+ caracteres |
| `EncryptedStringConverter` | `AttributeConverter` con inyección por constructor (Hibernate pide los conversores a Spring) |
| `CryptoProperties` | `vitsync.crypto.*`: `key`, `key-version`, `blind-index-pepper`, `previous-keys.<v>`, `allow-insecure-dev-key` |
| `CryptoConfig` | Registra los beans. Si una validación falla, **la aplicación no arranca** |
| `EncryptionException` | No comprobada. Su mensaje nombra la propiedad que falla, nunca el valor |

Configuración:

- `application.properties`: `${ENCRYPTION_KEY}`, `${ENCRYPTION_KEY_VERSION}` y
  `${BLIND_INDEX_PEPPER}`, sin valores por defecto.
- Perfil `local` y tests: clave de 32 bytes a cero y un pepper sin entropía, públicos a
  propósito (gitleaks no los marca y nadie los confunde con un secreto).
- Los valores de test van en `src/test/resources/config/application.properties`: un
  `application.properties` en la raíz de `test/resources` **taparía** el de `src/main`.

### Decisiones menores (sin ADR)

| Decisión | Motivo |
|---|---|
| La versión (`v1`) entra como **AAD** de GCM | Si alguien cambia el prefijo en la BD, el tag deja de cuadrar. Coste cero |
| Las claves anteriores se configuran como `VITSYNC_CRYPTO_PREVIOUS_KEYS_V<n>` | Es el *relaxed binding* de Spring para un mapa; no hace falta otra variable con formato propio |
| `KeyRing` borra el array de bytes tras crear la clave | `SecretKeySpec` hace una copia: el original no tiene por qué quedarse en memoria |
| `toString()` propio en `CryptoProperties` y `KeyRing` | El de un record imprime todos sus campos, y los `toString()` acaban en los logs |
| Las clases de cifrado no llevan anotaciones de Spring | Se prueban con un `new`, sin contexto; los beans se crean en `CryptoConfig` |

### Tests

`FieldEncryptorTest` (10), `BlindIndexTest` (5) y `KeyRingTest` (8), sin Spring:
ida y vuelta, prefijo de versión, cifrados distintos para el mismo valor, manipulación
detectada, prefijo autenticado, versión anterior descifrable, versión desconocida,
normalización del blind index y cada validación de `KeyRing`. `CryptoConfigTest` (4) usa
`ApplicationContextRunner`: arranca con configuración válida y falla sin clave, sin pepper o
con la clave de desarrollo no permitida.

`./mvnw verify` → **46 tests en verde**.

### Dejado fuera a propósito

- **"El NIF es ilegible en la BD"**: se prueba en el PR del registro, con la entidad `User`.
  Hibernate valida el esquema y no admite una entidad de prueba sin tabla.
- **Tarea de recifrado tras una rotación**: `needsRotation` ya existe; el proceso que la usa
  entra cuando haya datos que rotar.

---

## 2026-10-22 — Plan del Sprint 1: ocho decisiones antes de programar

### Contexto

Antes de la primera slice (registro y login) quedaban decisiones abiertas que el backlog,
los ADR y la guía no cerraban, o en las que se contradecían. Se cierran aquí para que el
sprint empiece con el alcance fijo. Rama `chore/sprint1-plan`; no hay código.

### Decisiones

| # | Decisión | Elegido | Motivo |
|---|---|---|---|
| 1 | Gestor de tareas | **Jira Cloud** | Es lo que se usa en empresa y el backlog ya se vuelca con `scripts/jira/bootstrap.mjs`. Montarlo sigue pendiente (Q9) |
| 2 | Frontend en el Sprint 1 | **No: solo backend** | La autenticación ya llena el sprint. El proyecto React arranca en el Sprint 2, con el contrato OpenAPI más rico |
| 3 | Recuperación de contraseña | **Código de un solo uso por email** | Las preguntas de seguridad de v1 se adivinan o se encuentran en redes sociales; NIST SP 800-63B desaconseja usarlas |
| 4 | Segundo factor (2FA) | **Sprint 4** | Como el roadmap de la guía; el backlog lo tenía en el Sprint 1 |
| 5 | Versionado de la API | **Prefijo `/api/v1` en cada controlador** | Cierra Q12. El versionado nativo de Spring 7 afecta a todas las rutas y choca con `/v3/api-docs` |
| 6 | Contraseñas filtradas (HaveIBeenPwned) | **Sí, sin bloquear si el servicio cae** | Se rechazan contraseñas filtradas (ADR-0004), pero el registro no depende de un tercero: si no responde, aviso en el log y se continúa. En tests se simula |
| 7 | Envío de emails | **Outbox desde el Sprint 1** | Verificación y recuperación necesitan email. Enviarlo directo sería el fallo P9 de v1 y un cambio "temporal" prohibido por la DoD |
| 8 | Primer administrador | **Usuarios demo en el perfil `local`** | Un admin, un médico y un paciente con datos sintéticos, solo en `local`. Cómo se crea en producción se decide en el Sprint 8 |

### Cambios en el backlog (`scripts/jira/backlog.json`)

| Historia | Antes | Ahora |
|---|---|---|
| Segundo factor por email en el login | S1 | **S4** |
| Pantallas de login y registro en React | S1 | **S2** |
| Cliente HTTP tipado con refresh single-flight | S1 | **S2** |
| Esqueleto del frontend React + TypeScript + Tailwind | S0 (no se hizo) | **S2** |
| Cifrado en reposo AES-256-GCM con versión de clave | S4 | **S1**: el registro ya guarda el NIF cifrado |
| Blind index para búsqueda por NIF cifrado | S4 | **S1**: el registro detecta NIF duplicados por el hash |
| Recuperación de cuenta por preguntas de seguridad | S1 | **Reescrita**: "Recuperación de contraseña por código de un solo uso por email" |
| Outbox transaccional y envío de emails con reintentos | — | **Nueva, S1** |
| Usuarios de demostración en el perfil local | — | **Nueva, S1** |

También se actualizan los objetivos de S1, S2 y S4, y el criterio del seed de S2 (los
usuarios demo ya existen desde S1). Comprobado con `node scripts/jira/bootstrap.mjs --dry-run`:
9 épicas, 53 historias, 260 puntos.

**A vigilar**: el Sprint 1 queda en **46 puntos**, bastante por encima del resto (18–37).
Si a mitad de sprint no da, lo primero que sale es la recuperación de contraseña, que no
bloquea a nada posterior.

### Orden de trabajo del Sprint 1 (un PR por paso)

1. Cifrado base en `common/crypto`: `AttributeConverter` AES-256-GCM con prefijo de versión
   y blind index HMAC-SHA256; la aplicación no arranca sin claves (ADR-0005).
2. Registro de pacientes: `User` y `PatientProfile`, `POST /api/v1/auth/register` con NIF
   validado, HaveIBeenPwned y 409 sin revelar qué campo está duplicado.
3. Outbox con su worker y verificación de email: migración `V2` con una tabla de códigos de
   un solo uso (con hash, caducidad e intentos), que el esquema V1 no tiene, y
   `POST /api/v1/auth/verify`.
4. Login: JWT RS256 de 15 minutos y refresh en cookie `HttpOnly`, respuestas uniformes, y
   los usuarios demo del perfil `local`.
5. Refresh rotativo con detección de reuso y registro en `audit_logs` (ADR-0004).
6. Recuperación de contraseña por código, revocando todas las sesiones.

### Desviaciones respecto a la guía

El roadmap de la guía pone en el Sprint 1 "front con login y ruta protegida". Pasa al
Sprint 2 (decisión 2). La guía no se edita: el plan vigente es este y el backlog.

---

## 2026-10-22 — Estructura de módulos y reglas de arquitectura (ArchUnit)

### Contexto

Paso 3 del "siguiente paso concreto" del [HANDOFF](HANDOFF.md) (guía, Fase 4): crear los
módulos del monolito modular y que sus fronteras las compruebe un test, no la memoria.
Rama `feat/module-structure`.

### Cambios realizados

**1. Seis módulos bajo `com.vitsync`**, cada uno con un `package-info.java` que dice de qué
se ocupa y de qué tablas es dueño:

| Módulo | Dueño de |
|---|---|
| `iam` | `users`, `patient_profiles`, `doctor_profiles`, `refresh_tokens` |
| `catalog` | `specialties`, `facilities`, `doctor_facilities` |
| `scheduling` | `schedule_rules`, `schedule_exceptions`, `appointments` |
| `clinical` | `medical_reports`, `report_files` |
| `gdpr` | `user_consents`, `gdpr_requests` |
| `notification` | `outbox_messages` |

`messaging` (chat) no se crea: es de v2.1 según [SCOPE](SCOPE.md). Las capas internas
(`api`, `application`, `domain`, `infrastructure`) aparecen con su primera clase; las reglas
ya las contemplan aunque no existan.

**2. ArchUnit 1.5.1** (`com.tngtech.archunit:archunit`, scope test). Se usa el artefacto base
y no `archunit-junit5` a propósito: Boot 4 trae JUnit 6 y el motor JUnit de ArchUnit está
hecho para la plataforma anterior. Las reglas son tests JUnit normales.

**3. `ArchitectureTest`** — cinco reglas, 10 casos (una se ejecuta por módulo):

| Regla | Qué impide |
|---|---|
| Capas en una sola dirección | Que `api` use `infrastructure`, o que algo use `api` |
| Un módulo solo usa de otro su `application` | Usar controladores, entidades o repositorios ajenos |
| `common` no depende de ningún módulo | Que la base compartida cree ciclos |
| `domain` sin web ni Spring Data | Lógica de negocio acoplada a HTTP o a la persistencia |
| Módulos sin ciclos | Dos módulos que en realidad son uno mal partido |

Comprobado en ambos sentidos: en verde sin violaciones, y en rojo, señalando clase y campo,
al introducir a propósito un controlador que usa un repositorio y un módulo que usa la
entidad de otro.

`./mvnw verify` → **19 tests en verde**.

### Consecuencia que hay que decidir: referencias entre módulos por id

La regla de módulos prohíbe que una entidad de un módulo referencie una entidad de otro. El
ejemplo de la Fase 5 de la guía lo hace (`Appointment` → `@ManyToOne PatientProfile`, de
`iam`). Ver Q13, resuelta en [ADR-0009](adr/0009-referencias-entre-modulos-por-id.md).

---

## 2026-10-21 — Contrato de API: errores RFC 9457 y OpenAPI versionado

### Contexto

Punto 2 del "siguiente paso concreto" del [HANDOFF](HANDOFF.md): contrato antes que código
(guía, Fase 3). Faltaban springdoc, el formato único de error y una forma de que el
contrato quedara versionado y vigilado en CI. Rama `feat/api-contract-foundation`.

### Decisión: springdoc 3.1.1

Cierra el único "a vigilar" del [ADR-0001](adr/0001-stack-tecnologico.md). La línea 2.x de
springdoc es la de Boot 3; la 3.x es la de Boot 4 / Spring Framework 7. Se fija **3.1.1**
porque su POM padre se construye contra **Spring Boot 4.1.0**, la misma línea que el
proyecto (4.1.1). Boot no gestiona su versión: va como propiedad `springdoc.version` en el
`pom.xml`.

### Cambios realizados

**1. Formato único de error (RFC 9457)** — paquete `com.vitsync.common.error`

Toda respuesta de error es `application/problem+json` con los campos estándar y dos
extensiones propias:

| Campo | Para qué |
|---|---|
| `code` | Nombre de la constante de `ErrorCode`. Es contrato: el front decide por él, no por el texto |
| `traceId` | Id de la petición, el mismo que aparece en los logs |

- `ErrorCode`: catálogo con estado HTTP, título y detalle. Solo los transversales
  (`VALIDATION_FAILED`, `MALFORMED_REQUEST`, `UNAUTHENTICATED`, `ACCESS_DENIED`,
  `RESOURCE_NOT_FOUND`, `INTERNAL_ERROR`); los de cada funcionalidad entran con ella.
- `BusinessException`: lo que lanzan los servicios cuando se incumple una regla.
- `GlobalExceptionHandler`: punto único de traducción excepción → respuesta. Cubre también
  los errores del propio Spring (404, 405, JSON ilegible…) para que todo error tenga la
  misma forma. **El mensaje de una excepción no llega nunca al cliente** (fuga V15 de v1);
  un 500 devuelve un texto genérico y la traza completa va al log.
- `type` de cada error: `https://api.vitsync.es/errors/<code-en-kebab-case>`.

**2. Trazabilidad por petición** — `com.vitsync.common.web.TraceIdFilter`

Primer filtro de la cadena, antes que la seguridad, para que también los 401 lleven traza.
Respeta un `X-Request-Id` entrante si es válido (`[A-Za-z0-9-]{8,64}`, para evitar inyección
en logs) o genera uno; lo devuelve en la cabecera y lo deja en el MDC. El patrón de log lo
imprime en cada línea (`logging.pattern.correlation`).

**3. Seguridad base** — `com.vitsync.common.config.SecurityConfig`

Cerrado por defecto: solo son públicos `/actuator/health/**`, `/v3/api-docs/**` y Swagger
UI. API sin estado, sin CSRF, formulario, HTTP Basic ni logout. Los 401 y 403 se delegan en
`GlobalExceptionHandler`, así que tienen el mismo formato. `@EnableMethodSecurity` activa
`@PreAuthorize`. Todavía no hay login: llega con la primera slice.

**4. OpenAPI** — `com.vitsync.common.config.OpenApiConfig`

Título, versión, servidor relativo (`/`, para que la spec no dependa del host) y esquema
`bearerAuth` (JWT) aplicado por defecto. `/v3/api-docs` y Swagger UI están **apagados por
defecto** y se encienden en el perfil `local` y en los tests.

**5. Contrato versionado y vigilado** — `docs/openapi.json` + `OpenApiContractTest`

El test genera la spec y la compara, como árbol JSON, con el fichero versionado. Si
difieren, falla. Corre dentro de `./mvnw verify`, así que **la CI ya detecta el drift sin
tocar el workflow**. Para regenerar el fichero tras un cambio de API:

```bash
./mvnw verify -Dopenapi.update=true
```

Hoy `paths` está vacío: no hay endpoints todavía.

**6. Paginación por defecto**

`spring.data.web.pageable.default-page-size=20` y `max-page-size=100`.

### Tests

| Test | Qué protege |
|---|---|
| `ProblemDetailContractTest` (7, `@WebMvcTest`, sin Docker) | 401, 403, regla de negocio, validación con lista de campos, 500 sin fuga de mensaje, errores del framework con `code`, eco de `X-Request-Id` |
| `OpenApiContractTest` (Testcontainers) | Que `docs/openapi.json` coincide con el código |
| `VitSyncApplicationTests` | Migraciones y mapeo contra PostgreSQL real (sin cambios) |

`./mvnw verify` → **9 tests en verde**.

### Dejado fuera a propósito

- **Endpoints del MVP como stubs 501** (entregable de la Fase 3 en la guía): tienen más
  sentido cuando exista la estructura de módulos (Fase 4, siguiente paso).
- **Esquema `Problem` documentado en cada operación del OpenAPI**: sin endpoints no hay
  dónde colgarlo; entra con el primero.
- **Versionado nativo de Spring Framework 7**: ver Q12.

---

## 2026-10-01 — Auditoría de configuración del repositorio

### Contexto

Revisión del repositorio tal y como está, buscando configuración o estructura incorrecta: no
se añadió funcionalidad, solo se comprobó que lo que los documentos afirman se cumple de
verdad al ejecutarlo. Salieron seis cosas, una de ellas bloqueante para la CI.

### 1. `apps/api/mvnw` estaba versionado sin permiso de ejecución — CI rota

**El fallo.** El fichero estaba en el índice de git como `100644`. El job `api` del workflow
ejecuta `./mvnw -B verify` en un runner Ubuntu, y ahí eso termina en
`./mvnw: Permission denied`: la CI habría fallado en su primera ejecución, antes de compilar
nada. No se había visto porque en Windows `git config core.fileMode` es `false` y el wrapper
se invoca a través de `mvnw.cmd`.

**La corrección.** `git update-index --chmod=+x apps/api/mvnw`; el fichero queda como
`100755`. Es un cambio en el índice, no en el contenido.

Esto bloqueaba también Q10: la protección de `master` se iba a configurar "una vez la CI haya
corrido al menos una vez", y la CI no podía pasar.

### 2. El arranque en local no funcionaba con lo que dice el README

**El fallo.** `docker compose up -d && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
reventaba con `FATAL: password authentication failed for user "vitsync"`. Es Q8, que estaba
anotada como "mitigada" pero cuya mitigación era manual: exportar `DB_PORT` en cada shell.
Lo que pasa por debajo, comprobado con `Get-NetTCPConnection`:

| Proceso | Escucha en |
|---|---|
| `postgres` nativo (servicio de Windows) | `0.0.0.0:5432` |
| `com.docker.backend` (el contenedor) | `[::]:5432` |

Los dos arrancan sin error —cada uno coge una familia de direcciones— pero el driver JDBC
resuelve `localhost` a IPv4 primero y acaba hablando con el PostgreSQL nativo, que no conoce
al usuario `vitsync`. El síntoma parece un problema de credenciales y es un problema de
puerto.

**La corrección.** El puerto publicado por defecto pasa a ser el **5433** en
`docker-compose.yml` y en el perfil `local`, que siguen leyendo `DB_PORT` para quien
necesite otro. Dentro de la red de compose nada cambia: el contenedor sigue en el 5432. Con
esto el arranque documentado funciona sin exportar nada. **Q8 queda cerrada.**

### 3. `CONTRIBUTING.md` documentaba un comando que no existe

El apartado de estilo presentaba `./mvnw spotless:apply` y `spotless:check` como el formateo
del proyecto y "lo que ejecuta la CI". No hay plugin de Spotless en el `pom.xml` ni paso de
Spotless en el workflow: los dos comandos fallan. Reescrito para decir lo que es verdad —que
el plugin entra en la Fase 11 con el resto del pipeline de calidad— en lugar de añadirlo
ahora, que es justo lo que el propio `ci.yml` razona que no hay que hacer.

### 4. `main` / `master`

La rama por defecto del repositorio es `master`, y es la que dispara la CI. El HANDOFF (D13)
y la guía hablaban de `main` en las reglas de protección de rama: seguir esas instrucciones
al pie de la letra protege una rama que no existe. Corregido en el HANDOFF; en la guía se
añade una nota en 0.3, porque es el documento de plan y se mantiene como se escribió.

### 5. Dos omisiones menores

- `docs/VADEMECUM.md` estaba en la tabla de documentación del README pero no en el árbol del
  repositorio. Añadido.
- `DB_PORT` lo leen `docker-compose.yml` y el perfil `local`, pero no aparecía en
  `.env.example`, que es el contrato de variables. Añadido en una sección aparte, marcado
  como de desarrollo (docker compose carga `.env` automáticamente).

### Inventario de ficheros

Todo modificado; ninguno creado ni borrado.

| Fichero | Cambio |
|---|---|
| `apps/api/mvnw` | Modo `100644` → `100755` en el índice de git (contenido intacto) |
| `docker-compose.yml` | Puerto del host de `db`: `${DB_PORT:-5432}` → `${DB_PORT:-5433}`, con el porqué en el comentario |
| `apps/api/src/main/resources/application-local.properties` | `${DB_PORT:5432}` → `${DB_PORT:5433}` en la URL de JDBC |
| `.env.example` | Sección "Desarrollo local" con `DB_PORT` |
| `CONTRIBUTING.md` | Spotless marcado como no configurado; tabla de servicios al 5433; sección del choque de puertos reescrita |
| `README.md` | Nota del 5433 en "Arrancar en local"; `VADEMECUM.md` en el árbol del repo |
| `docs/HANDOFF.md` | D13 y §6: `main` → `master` |
| `docs/GUIA_CONSTRUCCION.md` | Nota en 0.3: la rama real es `master` |

### Lo que se revisó y está bien

Para no volver a mirarlo: el `pom.xml` (los starters de Boot 4 y el BOM de Testcontainers
resuelven), el DDL de `V1` (19 tablas, el orden de las FK resuelve), `.gitignore` (ni
`target/` ni `.idea/` están versionados), los enlaces relativos de los 16 ficheros Markdown
(ninguno roto), `scripts/jira/bootstrap.mjs` (`--dry-run` imprime las 9 épicas) y el
healthcheck de Mailpit, que no está en el compose porque lo trae la propia imagen: el
contenedor aparece `healthy`, así que la afirmación de "healthcheck en los tres" se sostiene.

### Lo que sigue mal y no se ha tocado

- **Q5, `.idea/`**: el proyecto está abierto en IntelliJ como módulo Java plano, no como
  proyecto Maven, y con `languageLevel="JDK_25"` contra un proyecto de Java 21. De ahí los
  errores en el editor. No se arregla desde fuera —hay que reimportar `apps/api/pom.xml`
  como proyecto Maven— y no afecta al repositorio, porque `.idea/` está ignorado.
- **Q7, licencia**: sigue sin fichero `LICENSE`.
- **Docker Desktop estaba parado** al empezar la sesión, y por eso `./mvnw verify` fallaba
  con `Could not find a valid Docker environment`. No es un problema del repositorio, pero
  el mensaje de Testcontainers no ayuda: conviene descartarlo antes de buscar la causa en
  otro sitio.

### Verificación

Ejecutado, no solo escrito. Con Docker Desktop arrancado:

| Comprobación | Resultado |
|---|---|
| `./mvnw -B verify` | **verde** — 1 test, 37 s, Flyway aplica V1 sobre `postgres:16-alpine` desde cero |
| `docker compose up -d` | `db`, `minio` y `mailpit` los tres `healthy`; `db` publicado en `0.0.0.0:5433->5432` |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | arranca en 7,0 s **sin exportar `DB_PORT`** (antes de la corrección: `password authentication failed`) |
| `GET /actuator/health` | `HTTP 200`, con `db` y `mail` en `UP` |
| `flyway_schema_history` | V1 aplicada con `success = t`; 20 tablas en `public` |
| `node scripts/jira/bootstrap.mjs --dry-run` | las 9 épicas con sus historias |
| `git ls-files -s apps/api/mvnw` | `100755` |

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
| Q3 | **`apps/web/` no existe** | Pendiente | Corresponde a la Fase 9. La estructura ya reserva el hueco, y el job `web` de la CI se añadirá con él. |
| Q5 | **IntelliJ apunta al módulo antiguo** | Acción manual | `.idea/` referencia la ruta anterior. Recargar abriendo `apps/api/pom.xml` como proyecto Maven (o *File → Invalidate Caches*). No afecta al repo: `.idea/` está ignorado. |
| Q7 | **Licencia** | Abierta | MIT previsto, sin decidir. Mientras no exista el fichero `LICENSE`, el repositorio es legalmente "todos los derechos reservados" aunque esté público. Es lo único del Sprint 0 que sigue sin cerrar. |
| Q9 | **Tablero de Jira sin montar** | Pendiente | [JIRA_SETUP.md](JIRA_SETUP.md) y `scripts/jira/backlog.json` están listos; falta ejecutarlo. Es una acción manual con credenciales, no automatizable desde aquí. |

## Dudas resueltas

| # | Duda | Resolución |
|---|---|---|
| Q1 | Spring Boot 4.1.1 vs 3.5 | **4.1.1**, con la documentación alineada — [ADR-0001](adr/0001-stack-tecnologico.md) · 2026-09-12 |
| Q2 | `./mvnw verify` no pasaba | **Resuelta**: Testcontainers 1.21.4 + `@ServiceConnection`; verify en verde · 2026-09-12 |
| Q4 | `docker-compose.yml` no existía | **Resuelta**: creado y verificado con los tres servicios `healthy` · 2026-09-12 |
| Q6 | Monorepo vs dos repos | **Monorepo** — [ADR-0002](adr/0002-monorepo-frente-a-dos-repositorios.md) · 2026-09-12 |
| Q8 | Colisión en el puerto 5432 | **Resuelta**: el contenedor se publica en el 5433 por defecto (`DB_PORT` para cambiarlo); el arranque documentado funciona sin exportar nada · 2026-10-01 |
| Q11 | `./mvnw` versionado sin permiso de ejecución | **Resuelta**: `100755` en el índice; desbloquea la primera ejecución de la CI y con ella Q10 · 2026-10-01 |
| Q10 | Protección de `master` sin configurar | **Resuelta**: ruleset activo sobre `master` con PR obligatorio, checks `api (build + tests)` y `escaneo de secretos` obligatorios, historial lineal y sin force-push ni borrado · 2026-10-22 |
| Q12 | Versionado de API: prefijo `/api/v1` o el nativo de Spring Framework 7 | **Prefijo `/api/v1`** en el `@RequestMapping` de cada controlador; el nativo afecta a todas las rutas y choca con `/v3/api-docs` · 2026-10-22 |
| Q13 | Referencias entre módulos: por id o por entidad JPA | **Por id**, con la FK en la base de datos y los datos pedidos a la capa `application` del otro módulo — [ADR-0009](adr/0009-referencias-entre-modulos-por-id.md) · 2026-11-11 |
