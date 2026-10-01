# ADR-0003: Sin herencia JPA en el modelo de usuario

## Estado

**Aceptado** — 2026-09-12

## Contexto

En v1, `User` usaba `@Inheritance(InheritanceType.JOINED)` con tres subtipos: `Paciente`,
`Medico` y `Administrador`. Cada uno tenía su propia tabla, ligada a `usuarios` por la PK.

Dos consecuencias medidas en v1:

1. **Consultas polimórficas caras.** Cualquier carga de `User` generaba `LEFT JOIN` contra
   las tres subtablas, incluso cuando solo hacía falta el email para autenticar.
2. **Filas huérfanas.** Al sembrar médicos insertando solo en `usuarios`, quedaron usuarios
   con rol médico sin fila en `medicos`. Hubo que escribir el script
   `FIX01__backfill_medicos_rows.sql` para reparar los datos en producción. La herencia JPA
   no obliga a que la subfila exista: la integridad dependía de que el código lo hiciera
   bien siempre, y no lo hizo.

El problema de fondo es que la herencia modelaba **roles**, que son un atributo del usuario y
pueden cambiar, como si fueran **tipos**, que no cambian.

## Opciones consideradas

1. **`JOINED` (lo de v1)** — una tabla por subtipo, unidas por PK.
   - `+` Esquema normalizado, sin columnas nulas.
   - `−` Joins polimórficos en toda consulta de `User`.
   - `−` Sin garantía de que la subfila exista; ya falló una vez.
   - `−` Cambiar el rol de un usuario implica borrar de una subtabla e insertar en otra.

2. **`SINGLE_TABLE`** — una tabla con columna discriminadora.
   - `+` Sin joins, consultas rápidas.
   - `−` Todas las columnas específicas de cada rol tienen que ser nulas, así que se pierde
     la validación en la base de datos. Con datos clínicos de por medio, es inaceptable.

3. **Composición: `users` + tablas de perfil 1:1 opcionales** (elegida).
   - `+` Autenticar solo toca `users`.
   - `+` La FK con `PRIMARY KEY = FK a users(id)` garantiza la relación 1:1 en la base de
     datos, no en el código.
   - `+` Un usuario puede tener más de un perfil si algún día hace falta (un médico que
     además es paciente del sistema).
   - `−` Hay que crear el perfil explícitamente al dar de alta, dentro de una transacción.

## Decisión

**Sin herencia JPA.** El modelo es:

- `users` — identidad y autenticación: email, hash de contraseña, `role`, `status`, nombre,
  NIF cifrado, teléfono cifrado, fecha de nacimiento, verificación, 2FA.
- `patient_profiles` — datos clínicos del paciente. `PRIMARY KEY = FK a users(id)`.
- `doctor_profiles` — número de colegiado, especialidad, biografía, foto.
  `PRIMARY KEY = FK a users(id)`.

El rol se resuelve por la columna `users.role` (`PATIENT | DOCTOR | ADMIN`, con `CHECK`) y,
cuando hace falta el detalle, por la existencia del perfil correspondiente.

En JPA, `PatientProfile` y `DoctorProfile` son entidades independientes con
`@MapsId` sobre la relación a `User`. **Ninguna extiende a `User`.**

El alta de un usuario con perfil se hace en **un único servicio transaccional**, de modo que
no puede quedar un usuario con rol `DOCTOR` sin `doctor_profiles`.

## Consecuencias

- `+` Las consultas de autenticación y de listado son planas y predecibles.
- `+` La integridad 1:1 la impone PostgreSQL, no una convención del equipo.
- `+` Cambiar el rol de un usuario es un `UPDATE` sobre una columna más la creación del
  perfil nuevo; el perfil antiguo puede conservarse.
- `−` Hay que acordarse de crear el perfil en el alta. Se mitiga con el servicio
  transaccional y con un test de integración que lo cubre.
- `−` Consultar "todos los médicos con su usuario" requiere un join explícito. Es un join
  que se escribe a conciencia, no uno que aparece solo.
- `−` **A vigilar**: si en el futuro se añade un cuarto rol, hay que decidir si necesita
  tabla de perfil propia o si le basta con `users`.
