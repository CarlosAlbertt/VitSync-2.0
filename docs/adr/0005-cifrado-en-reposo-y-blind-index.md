# ADR-0005: Cifrado en reposo AES-256-GCM y blind index

## Estado

**Aceptado** — 2026-09-12

## Contexto

VitSync trata **datos de salud**, categoría especial del Art. 9 del RGPD. El Art. 32 exige
medidas técnicas apropiadas al riesgo, y para datos de salud el cifrado en reposo es el
mínimo esperable, no un extra.

v1 ya cifraba con un `AttributeConverter` de JPA, y el enfoque era correcto. Lo que le
faltaba:

- La clave venía de configuración sin un mecanismo de **rotación**: cambiarla habría hecho
  ilegibles todos los datos existentes.
- No había forma de **buscar** por un campo cifrado. Con AES-GCM, cifrar dos veces el mismo
  NIF produce cifrados distintos (el IV es aleatorio), así que `WHERE nif_encrypted = ?` no
  funciona. La alternativa —descifrar la tabla entera en memoria y filtrar— no escala y
  expone los datos en el proceso.

## Opciones consideradas

### Dónde cifrar

1. **Cifrado de disco / TDE del proveedor** (Neon, RDS).
   - `+` Cero código.
   - `−` Protege contra el robo del disco físico, no contra una fuga de la base de datos:
     cualquiera con credenciales de conexión lee todo en claro. Insuficiente para Art. 9.

2. **Cifrado en la aplicación, por columna** (elegida).
   - `+` Los datos llegan ya cifrados a la base de datos. Un volcado robado es ilegible sin
     la clave, que vive en otro sitio.
   - `−` Los campos cifrados no se pueden indexar, ordenar ni buscar directamente.

3. **`pgcrypto` en la base de datos.**
   - `−` La clave viaja en la sentencia SQL y acaba en los logs de PostgreSQL.

### Cómo buscar sobre un campo cifrado

1. **Cifrado determinista** para los campos buscables.
   - `−` El mismo valor produce siempre el mismo cifrado: permite análisis de frecuencia y
     confirmar si un NIF concreto está en la base de datos.
2. **Blind index: columna extra con HMAC-SHA256 del valor normalizado** (elegida).
   - `+` Permite `WHERE nif_hash = ?` con un índice único normal.
   - `−` Sigue permitiendo comprobar la existencia de un valor conocido si se filtran a la
     vez la base de datos y el *pepper*.

## Decisión

**Cifrado**: **AES-256-GCM** (cifrado autenticado: detecta manipulación, no solo oculta) vía
`AttributeConverter` de JPA, de modo que la entidad trabaja con texto plano y la columna
guarda el cifrado. Las columnas cifradas llevan el sufijo **`_encrypted`** en el esquema.

Qué se cifra: NIF, teléfono, tarjeta sanitaria, grupo sanguíneo, alergias, patologías,
contacto de emergencia, notas de informes y cuerpos de mensajes.

**Rotación de clave**: el valor almacenado lleva **prefijo de versión** — `v1:base64...`. Al
descifrar se elige la clave por la versión; al cifrar se usa siempre la versión actual
(`ENCRYPTION_KEY_VERSION`). Rotar consiste en publicar la clave nueva, subir la versión y
recifrar en segundo plano. **Sin este prefijo, rotar es imposible sin parar el servicio**: es
la corrección concreta al diseño de v1.

**Búsqueda**: los campos que hay que buscar llevan una columna hermana **`_hash`** con
**HMAC-SHA256** del valor normalizado (trim, mayúsculas) usando un *pepper* dedicado
(`BLIND_INDEX_PEPPER`), distinto de la clave de cifrado. Hoy aplica a `users.nif_hash`, con
índice único.

**Claves**: `ENCRYPTION_KEY`, `ENCRYPTION_KEY_VERSION` y `BLIND_INDEX_PEPPER` vienen del
entorno, nunca del repositorio, y la aplicación **no arranca** si falta alguna.

**Prohibiciones**: ningún dato clínico en logs, en URLs, ni en el campo `details` de
`audit_logs`.

## Consecuencias

- `+` Un volcado de la base de datos filtrado no expone datos de salud.
- `+` La rotación de clave es una operación planificable, no una migración de emergencia.
- `+` Se puede buscar por NIF sin descifrar la tabla.
- `−` Un campo cifrado no se puede ordenar, filtrar por rango ni usar en `LIKE`. Cualquier
  funcionalidad que lo necesite hay que rediseñarla, no parchearla.
- `−` El blind index revela igualdad: dos usuarios con el mismo NIF son detectables, y quien
  tenga la base de datos **y** el *pepper* puede comprobar si un NIF concreto existe. Por eso
  el *pepper* se guarda separado de la clave de cifrado y de la base de datos.
- `−` Coste de CPU en cada lectura y escritura de esas columnas. Despreciable a esta escala.
- `−` **A vigilar**: perder `ENCRYPTION_KEY` significa perder los datos. La custodia y copia
  de seguridad de las claves es parte del procedimiento de despliegue, no un detalle.
- `−` **A vigilar**: cambiar el *pepper* del blind index obliga a recalcular todos los hashes.
