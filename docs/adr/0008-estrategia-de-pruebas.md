# ADR-0008: Testcontainers en vez de H2, y pirámide de pruebas

## Estado

**Aceptado** — 2026-09-12

## Contexto

v1 ejecutaba sus tests contra **H2 en memoria** mientras producción corría sobre
**PostgreSQL** (problema P7 de la guía). Eso significa que la suite verde no demostraba nada
sobre el sistema real: H2 y PostgreSQL difieren en tipos (`citext`, `jsonb`, `inet`,
`timestamptz`), en índices parciales, en el comportamiento de las secuencias y en la
semántica de los bloqueos.

Ese último punto es el decisivo para VitSync. La garantía anti-doble-reserva de v2 es un
**índice único parcial**:

```sql
CREATE UNIQUE INDEX ux_appointments_doctor_slot_active
    ON appointments (doctor_id, starts_at)
    WHERE status <> 'CANCELLED';
```

H2 no reproduce esa construcción, así que el test más importante del sistema —que dos
pacientes no puedan reservar el mismo hueco a la vez— **no se puede escribir contra H2**. Con
H2, el peor escenario no es un test que falla: es un test que pasa y no prueba nada.

Además, con Flyway como fuente de verdad del esquema y `ddl-auto=validate`, ejecutar las
migraciones contra un motor distinto del de producción invalida la validación entera.

## Opciones consideradas

1. **H2 en memoria** (lo de v1).
   - `+` Rápido y sin dependencias externas.
   - `−` No es el motor de producción. Ya produjo divergencias en v1.

2. **Una base de datos PostgreSQL compartida para los tests.**
   - `+` Motor real.
   - `−` Estado compartido entre ejecuciones y entre desarrolladores: los tests se contaminan
     entre sí y fallan según el orden. Y hay que montarla y mantenerla.

3. **Testcontainers: PostgreSQL efímero en Docker por ejecución** (elegida).
   - `+` El mismo motor y la misma versión que producción, arrancando desde cero.
   - `−` Requiere Docker en la máquina de desarrollo y en la CI.
   - `−` Unos segundos de arranque por suite.

## Decisión

**Testcontainers, nunca H2.** Los tests de integración corren contra
`postgres:16-alpine` — la misma imagen del `docker-compose` y la misma versión mayor que
producción — levantado por Testcontainers y conectado con `@ServiceConnection`, sin
configuración manual del `DataSource`.

**Pirámide** (guía, Fase 10):

| Nivel | Proporción | Qué cubre |
|---|---|---|
| Unitarios de dominio | ~50% | Invariantes: no reservar en el pasado, no cancelar una cita completada |
| Integración con BD real | ~40% | Un test por endpoint y por código de respuesta (200/400/401/403/404/409) |
| E2E (Playwright) | ~10% | Recorridos completos: registro → verificación → login → reserva → cancelación |

**Tipos de test obligatorios**, más allá de la proporción:

- **Concurrencia**: 10 reservas simultáneas del mismo hueco → exactamente un `201` y nueve
  `409`. Este test es la razón de ser de esta decisión.
- **Seguridad**: matriz rol × endpoint, y el caso "el usuario A accede al recurso de B → 403"
  (el IDOR sistémico V03 de v1).
- **Migraciones**: Flyway aplica desde cero y Hibernate valida el esquema resultante. Lo
  cubre ya `VitSyncApplicationTests`.
- **Arquitectura**: ArchUnit sobre `com.vitsync` — que ningún módulo importe los internos de
  otro y que las entidades no salgan de la capa de persistencia.

**Frontend**: Vitest + Testing Library por comportamiento observable, MSW para simular la API
contra el mismo contrato OpenAPI, y Playwright con `@axe-core/playwright` para accesibilidad.

**Cobertura**: JaCoCo con umbral en `application` y `domain` (80% líneas / 70% ramas). No se
persigue el 100%: se persigue que **cada regla de negocio tenga un test que la nombra**.

## Consecuencias

- `+` Una suite verde significa algo: el código ha funcionado contra el motor de producción.
- `+` Los índices parciales, los tipos de PostgreSQL y las migraciones quedan cubiertos por
  los tests, no comprobados a ojo.
- `+` Nadie tiene que montar una base de datos para ejecutar los tests: basta con Docker.
- `−` **Docker pasa a ser requisito** para `./mvnw verify`, en local y en CI. Queda escrito
  en el README y en `CONTRIBUTING.md`.
- `−` La suite tarda más que con H2. Se mitiga reutilizando el contenedor entre clases de
  test (una clase base común) en lugar de levantar uno por clase.
- `−` **A vigilar**: cuando la suite crezca, conviene separar `*Test` (unitarios, rápidos, en
  `surefire`) de `*IT` (integración, en `failsafe`), para que el ciclo corto de desarrollo no
  arrastre los contenedores.
