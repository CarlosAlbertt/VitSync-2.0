# ADR-0001: Stack tecnológico de v2

## Estado

**Aceptado** — 2026-09-12

## Contexto

v1 ([VITSYNC-API](https://github.com/CarlosAlbertt/VITSYNC-API) + VITSYNC-WebApp) funcionaba
sobre Spring Boot 3.2.5 / Java 21 / PostgreSQL 15 (Neon) y Vue 3 + JavaScript. El proyecto
cumplía su función, pero acumuló deuda estructural documentada en la guía (P1–P13): esquema
aplicado a mano en la consola de Neon, tests contra H2 mientras producción era PostgreSQL,
entidades serializadas directamente al cliente, documentación de API escrita a mano y
desincronizada del código.

v2 se reconstruye con objetivo **portfolio**: enseñar el proceso de ingeniería completo, no
solo el producto. Eso condiciona la elección: cada pieza tiene que ser defendible en una
entrevista técnica y reconocible para un revisor.

El esqueleto se generó con Spring Initializr, que hoy entrega **Spring Boot 4.1.1**. La guía
se había escrito sobre la línea 3.5, lo que abrió la duda Q1 en
[`DUDAS_Y_CAMBIOS.md`](../DUDAS_Y_CAMBIOS.md).

## Opciones consideradas

### Sobre la versión de Spring Boot

1. **Spring Boot 4.1.x** — línea actual, sobre Spring Framework 7.
   - `+` Es donde entra todo el desarrollo nuevo; 3.5 es la última línea 3.x y está en
     mantenimiento. Arrancar en 3.5 significa hacer la migración igual, pero más tarde y con
     código de producto encima.
   - `+` Starters modulares: se arrastra solo lo que se usa, en vez del
     `spring-boot-starter-test` monolítico que metía JUnit, Mockito, JSONassert, XMLUnit y
     Hamcrest en todos los módulos. En un monolito modular que va a crecer a nueve features,
     controlar el grafo de dependencias es lo que evita que el build se pudra.
   - `+` Versionado de API como primitiva del framework, en lugar de `/v1` como cadena en el
     path. Encaja con el contrato versionado de la guía (Fase 3.1).
   - `+` Anotaciones de nulidad (JSpecify) en toda la API, aprovechables por el IDE y por un
     analizador estático en CI.
   - `−` Los ejemplos de la guía se escribieron sobre 3.x y algunos necesitarán ajuste de
     sintaxis al llegar a su fase.
   - `−` Hay que verificar la línea compatible de springdoc.

2. **Spring Boot 3.5.x** — lo que asumía la guía.
   - `+` Todos los ejemplos de la guía aplican literalmente.
   - `+` Ecosistema asentado, cero sorpresas de compatibilidad.
   - `−` Se arranca un proyecto nuevo en una línea que ya no recibe funcionalidades.
   - `−` La migración a 4.x queda pendiente para cuando cueste más.

## Decisión

El stack de v2 es:

| Capa | Elección |
|---|---|
| Lenguaje / runtime | Java 21 (LTS) |
| Framework | **Spring Boot 4.1.1** (Spring Framework 7) |
| Build | Maven |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway como única fuente de verdad del esquema (guía, Fase 2.2) |
| Mapeo DTO | MapStruct + Lombok acotado |
| Contrato | OpenAPI 3 con springdoc; errores RFC 9457 (`ProblemDetail`) |
| Tests backend | JUnit 5 + Testcontainers ([ADR-0008](0008-estrategia-de-pruebas.md)) |
| Frontend | React 19 + TypeScript + Vite 7 |
| Estado front | TanStack Query (servidor) + Zustand (UI) |
| Estilos | Tailwind 4 + shadcn/ui |
| CI/CD | GitHub Actions |

Se mantienen de v1 **PostgreSQL, Spring Boot y Java**: la base era correcta y el objetivo de
aprendizaje es profundizar en ella, no cambiar de ecosistema. Se sustituye todo lo demás por
la opción que un equipo elegiría hoy.

El salto de Vue a React (decisión D2) se justifica por petición explícita y por demanda de
mercado; no es una crítica técnica a Vue.

## Consecuencias

- `+` El proyecto arranca en la línea con soporte y recorrido largo: no nace con una
  migración pendiente.
- `+` El grafo de dependencias queda explícito desde el primer commit.
- `−` La guía de construcción queda con ejemplos de la línea 3.x. Se corrigen fase a fase
  según se llega a ellos; el aviso está en la Parte B de la guía.
- `−` **A vigilar**: la versión de **springdoc** compatible con Boot 4 hay que fijarla al
  entrar en la Fase 3. Es la única dependencia del stack cuya compatibilidad no es
  independiente de la versión de Boot (MapStruct es procesamiento en tiempo de compilación,
  y Testcontainers y Flyway no dependen del framework).
- `−` Testcontainers ya no está gestionado por el BOM de Boot: su versión se fija importando
  `testcontainers-bom` en el `pom.xml`. Hay que actualizarla a mano.
