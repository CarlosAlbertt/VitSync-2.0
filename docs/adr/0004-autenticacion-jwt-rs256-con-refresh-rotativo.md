# ADR-0004: Autenticación con JWT RS256 y refresh opaco rotativo

## Estado

**Aceptado** — 2026-09-12

## Contexto

v1 ya usaba JWT RS256 con refresh persistido en base de datos, y fue de lo mejor resuelto del
proyecto. La auditoría de seguridad (hallazgos V01–V21) dejó, aun así, dos huecos:

- **El refresh no rotaba.** Un token robado servía durante toda su vida útil y no había forma
  de detectar que se estaba usando desde dos sitios.
- **No había señal de robo.** Sin rotación, el uso simultáneo del mismo refresh por el
  atacante y por el usuario legítimo es indistinguible de un uso normal.

Además, v1 guardaba el access token en `localStorage`, accesible desde cualquier XSS.

## Opciones consideradas

### Formato del access token

1. **JWT firmado con RS256** (elegida) — clave privada firma, pública verifica.
   - `+` Verificable sin consultar la base de datos en cada petición.
   - `+` La clave pública puede distribuirse a otros servicios sin darles capacidad de emitir.
   - `−` No es revocable antes de su expiración: se compensa con una vida corta (15 min).

2. **HS256** — un secreto compartido.
   - `−` Quien verifica puede emitir. Cierra la puerta a separar servicios más adelante.

3. **Token opaco con sesión en servidor**.
   - `+` Revocable al instante.
   - `−` Una consulta a la base de datos por petición y estado en el servidor; innecesario
     para el tamaño de este sistema.

### Formato del refresh token

1. **Opaco, aleatorio, persistido como hash** (elegida).
   - `+` Revocable, auditable, y si se filtra la base de datos no se obtienen tokens usables.
2. **JWT de larga duración**.
   - `−` No revocable, que es justo lo que se necesita de un refresh.

## Decisión

**Access token**: JWT **RS256**, vida **15 minutos**, claims mínimos — `sub` = `public_id`
del usuario (nunca el `id` interno, ver decisión D6), `role`, `jti`. Se guarda **solo en
memoria** del navegador: nunca en `localStorage` ni en `sessionStorage`.

**Refresh token**: valor **opaco** de 32 bytes aleatorios, vida **7 días**, entregado en
cookie `HttpOnly; Secure; SameSite=Lax`. En la base de datos se guarda **solo su SHA-256**
(`refresh_tokens.token_hash`), nunca el valor.

**Rotación con detección de reuso**: cada canje de refresh revoca el anterior y emite uno
nuevo con la **misma `family_id`**. Si llega un refresh **ya revocado**, se revoca **toda la
familia** y se registra el evento en `audit_logs`: es la firma de un token robado. Es lo que
hacen Auth0 y Okta, y es el hueco concreto que tenía v1.

**Segundo factor**: código de un solo uso por email, hasheado en base de datos, caducidad de
10 minutos (v1 ya lo tenía; se mantiene).

**Contraseñas**: BCrypt cost 12, mínimo 12 caracteres, y comprobación contra la lista de
contraseñas filtradas de HaveIBeenPwned por k-anonymity (solo viajan los 5 primeros
caracteres del SHA-1: la contraseña nunca sale del servidor).

**Respuestas uniformes en login**: mismo mensaje y tiempo de respuesta tanto si el email no
existe como si la contraseña es incorrecta, para no permitir enumeración de cuentas.

## Consecuencias

- `+` Un access token robado caduca en 15 minutos.
- `+` Un refresh robado se detecta en cuanto el usuario legítimo o el atacante lo reutiliza,
  y la reacción corta todas las sesiones de esa familia.
- `+` Con el access token solo en memoria, un XSS no puede exfiltrarlo desde el
  almacenamiento; y la cookie del refresh es `HttpOnly`, así que tampoco es legible por JS.
- `−` Al recargar la página el frontend se queda sin access token y tiene que hacer un
  refresh silencioso al arrancar. Es complejidad real en el cliente, asumida a cambio de no
  dejar el token en el almacenamiento.
- `−` La rotación obliga a manejar la concurrencia: dos peticiones que caducan a la vez
  intentan refrescar a la vez y la segunda encontraría el token ya revocado. El cliente debe
  **serializar los refresh** (una sola promesa compartida); si no, la detección de reuso
  producirá falsos positivos que cierran la sesión al usuario.
- `−` **A vigilar**: la tabla `refresh_tokens` crece. Hace falta una tarea de limpieza de
  tokens caducados y revocados.
- `−` Las claves RSA (`JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`) son secretos de despliegue: nunca
  en el repositorio. gitleaks lo vigila en CI — v1 llegó a tener credenciales en el
  historial.
