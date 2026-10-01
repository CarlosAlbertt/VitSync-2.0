# ADR-0007: SSE para notificaciones y STOMP para el chat

## Estado

**Aceptado** — 2026-09-12

## Contexto

v1 arrastraba **dos mecanismos de tiempo real a la vez** — WebSocket con STOMP para el chat
y sondeo periódico para las notificaciones — sin que ninguna decisión explicara por qué. El
resultado: dos rutas de código que mantener, dos formas de autenticar y dos fuentes de
errores para el mismo problema.

Además, la auditoría detectó el hallazgo **V08**: el emisor de un mensaje se tomaba del
*payload* que enviaba el cliente, lo que permitía suplantar a otro usuario simplemente
cambiando un campo del JSON.

v2 necesita tiempo real para dos casos con formas distintas:

- **Notificaciones** (cita confirmada, informe nuevo): unidireccionales servidor → cliente,
  poco frecuentes, y el MVP las necesita.
- **Chat paciente ↔ profesional**: bidireccional, interactivo, y está fuera del MVP (v2.1
  según `SCOPE.md`).

## Opciones consideradas

1. **Sondeo (polling)** — lo que hacía v1 para notificaciones.
   - `+` Trivial.
   - `−` Latencia o tráfico, elige uno. Y con TanStack Query ya hay refetch por foco de
     ventana, que cubre el 80% del caso sin llamarlo "tiempo real".

2. **SSE (`text/event-stream`)**.
   - `+` HTTP normal: atraviesa proxies, se autentica igual que el resto de la API.
   - `+` Reconexión automática nativa del navegador, sin librería en el cliente.
   - `−` Unidireccional. Y con HTTP/1.1 consume una de las ~6 conexiones por dominio (con
     HTTP/2 deja de ser un problema).

3. **WebSocket + STOMP**.
   - `+` Bidireccional y con semántica de suscripción por destino, que es lo que pide un chat.
   - `−` Protocolo aparte: su propia autenticación, su propio manejo de reconexión, y estado
     de conexión en el servidor que complica el escalado horizontal.

4. **Servicio externo de chat** (TalkJS, Stream).
   - `+` Cero mantenimiento.
   - `−` Los mensajes son datos clínicos: sacarlos a un tercero abre un encargo de tratamiento
     y una transferencia que hay que justificar en el registro del Art. 30. No compensa.

## Decisión

**Una tecnología por caso de uso, y cada una escrita aquí:**

| Caso | Tecnología | Cuándo |
|---|---|---|
| Notificaciones | **SSE** — `GET /api/v1/notifications/stream` | MVP (v2.0) |
| Chat | **WebSocket + STOMP** | v2.1 |

**Notificaciones (SSE)**: el flujo se autentica con el mismo access token que el resto de la
API. Cada usuario recibe solo sus propios eventos; el `userId` sale siempre del principal
autenticado. Los eventos los publica el worker de `outbox_messages`, de modo que una
notificación no se pierde si el usuario está desconectado: queda en la bandeja y el flujo
solo es el canal de entrega inmediata.

**Chat (STOMP, v2.1)**: la autenticación se hace en el frame **CONNECT** con el access token.
El emisor de cada mensaje se deriva **del principal de la sesión, nunca del payload** — es la
corrección directa de V08. La autorización se comprueba por pertenencia a
`conversation_members` antes de entregar nada.

**No se usa sondeo** para nada. Si un dato no justifica un canal en tiempo real, se refresca
con TanStack Query y ya está.

## Consecuencias

- `+` Un solo mecanismo por caso, y ambos justificados por escrito: se acaba la duplicidad
  de v1.
- `+` SSE no añade dependencias ni en el backend (`SseEmitter` de Spring) ni en el frontend
  (`EventSource` del navegador).
- `+` Apoyarse en el outbox hace que las notificaciones sean fiables aunque el canal no lo sea.
- `−` Mantener un flujo SSE abierto por usuario consume un hilo o una conexión. Con hilos
  virtuales de Java 21 el coste es bajo, pero hay que vigilarlo con Micrometer.
- `−` **A vigilar**: al escalar a más de una instancia, ni SSE ni STOMP entregan por sí solos
  a un usuario conectado a otra instancia. Hará falta un bus (Redis pub/sub o el broker STOMP
  externo). No se resuelve ahora: el MVP corre en una instancia, y forzar la solución antes
  de tiempo sería sobreingeniería.
- `−` Algunos proxies corporativos cortan conexiones largas. La reconexión nativa de
  `EventSource` lo absorbe, pero conviene un `heartbeat` periódico.
