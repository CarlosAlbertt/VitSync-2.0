# Alcance de VitSync 2.0

> Tres listas cerradas. Lo que no está en **MVP** no se construye ahora, por buena que sea la
> idea: se anota en *Siguiente* y se decide en su momento.
> Entregable de la Fase 0 de la [guía](GUIA_CONSTRUCCION.md#01-definir-el-alcance-del-mvp-y-lo-que-queda-fuera).

---

## MVP — v2.0

Lo que tiene que existir para que el producto sea demostrable de principio a fin.

| Área | Incluye |
|---|---|
| **Identidad** | Registro con verificación por email, login, segundo factor por email, recuperación de contraseña, cierre de sesión |
| **Catálogo** | Especialidades y cuadro médico, con búsqueda y filtros |
| **Citas** | Consulta de huecos disponibles, reserva, cancelación, listado del paciente y agenda del profesional |
| **Perfil** | Datos personales del paciente, datos clínicos básicos, cambio de contraseña |
| **Informes** | Listado, consulta y descarga de informes con adjuntos |
| **Administración** | Gestión de usuarios, profesionales y especialidades |
| **RGPD** | Consentimientos, derecho de acceso, exportación de datos y supresión con anonimización |
| **Notificaciones** | Avisos en la aplicación por SSE y email transaccional (cita confirmada, cita cancelada, informe nuevo) |

## Siguiente — v2.1

Diseñado para que quepa, pero no construido ahora.

- Chat paciente ↔ profesional (WebSocket + STOMP, [ADR-0007](adr/0007-estrategia-de-tiempo-real.md)).
- "Mi Salud": mediciones del paciente con gráficas de evolución.
- Generación de informes en PDF desde la aplicación.
- Panel de métricas para administración.

## Fuera de alcance

No se construye, y no se deja hueco para ello.

- Facturación y pagos.
- Integración con sistemas sanitarios (HL7 / FHIR).
- Aplicación móvil nativa.
- Multi-organización (varias clínicas aisladas en la misma instancia).
- Videoconsulta integrada: `appointments.modality = TELEMEDICINE` guarda una `meeting_url`
  de un proveedor externo, no una sala propia.

---

## Historias con criterios de aceptación

El backlog completo está en [`scripts/jira/backlog.json`](../scripts/jira/backlog.json)
(9 épicas). Aquí quedan los criterios de las reglas de negocio que no son evidentes y que
tienen que acabar siendo un test con el mismo nombre.

### Reserva de cita

```gherkin
Funcionalidad: Reserva de cita

  Escenario: Reserva correcta
    Dado un paciente autenticado y verificado
    Y un hueco libre del doctor D el 2026-09-01 a las 10:00
    Cuando solicita cita con el doctor D el 2026-09-01 a las 10:00
    Entonces recibe 201 Created con el publicId de la cita
    Y la cita queda en estado SCHEDULED

  Escenario: El hueco ya no está disponible
    Dado un paciente autenticado
    Y una cita activa del doctor D el 2026-09-01 a las 10:00
    Cuando solicita cita con el doctor D el 2026-09-01 a las 10:00
    Entonces recibe 409 Conflict con code "APPOINTMENT_SLOT_TAKEN"
    Y no se crea ninguna cita nueva

  Escenario: Dos pacientes compiten por el mismo hueco
    Dado un hueco libre del doctor D el 2026-09-01 a las 10:00
    Cuando 10 pacientes lo solicitan simultáneamente
    Entonces exactamente uno recibe 201 Created
    Y los otros nueve reciben 409 Conflict

  Escenario: No se reserva en el pasado
    Cuando un paciente solicita cita en una fecha anterior a ahora
    Entonces recibe 400 Bad Request con code "APPOINTMENT_IN_THE_PAST"
```

### Cancelación

```gherkin
Funcionalidad: Cancelación de cita

  Escenario: El paciente cancela su propia cita
    Dado un paciente con una cita en estado SCHEDULED
    Cuando la cancela
    Entonces recibe 204 No Content
    Y la cita pasa a CANCELLED con cancelled_at y cancelled_by informados
    Y el hueco vuelve a estar disponible

  Escenario: Un paciente intenta cancelar la cita de otro
    Dado un paciente A y una cita del paciente B
    Cuando A intenta cancelar la cita de B
    Entonces recibe 403 Forbidden
    Y la cita de B no cambia de estado

  Escenario: No se cancela una cita ya completada
    Dado una cita en estado COMPLETED
    Cuando se intenta cancelar
    Entonces recibe 409 Conflict con code "APPOINTMENT_NOT_CANCELLABLE"
```

### Acceso a informes

```gherkin
Funcionalidad: Acceso a informes médicos

  Escenario: El paciente descarga su informe
    Dado un paciente autenticado con un informe propio
    Cuando solicita la descarga del adjunto
    Entonces recibe 302 hacia una URL prefirmada válida 60 segundos
    Y el acceso queda registrado en audit_logs

  Escenario: Un usuario ajeno intenta acceder
    Dado un paciente A y un informe del paciente B
    Cuando A solicita el informe de B por su publicId
    Entonces recibe 403 Forbidden
    Y el intento queda registrado en audit_logs con success = false
```

### Sesión y refresh

```gherkin
Funcionalidad: Rotación del refresh token

  Escenario: Renovación normal
    Dado una sesión con un refresh válido
    Cuando se canjea
    Entonces recibe un access token nuevo y un refresh nuevo de la misma familia
    Y el refresh anterior queda revocado

  Escenario: Reuso de un refresh revocado
    Dado un refresh ya canjeado anteriormente
    Cuando se intenta canjear de nuevo
    Entonces recibe 401 Unauthorized
    Y se revoca la familia completa de tokens
    Y queda registrado en audit_logs
```

### Supresión RGPD

```gherkin
Funcionalidad: Derecho de supresión

  Escenario: El paciente solicita la supresión
    Dado un paciente autenticado
    Cuando solicita la supresión de su cuenta
    Entonces se crea una gdpr_requests de tipo ERASURE
    Y al ejecutarse, el usuario pasa a estado ANONYMIZED
    Y sus datos identificativos quedan borrados o seudonimizados
    Y sus adjuntos se borran del bucket
    Y las entradas de audit_logs se conservan con actor_ref seudonimizado
```

---

## Cómo se usa este documento

- Una petición nueva que no encaje en el MVP **no entra en el sprint**: se añade a *Siguiente*
  o a *Fuera*, y se decide después.
- Cambiar el alcance del MVP es una decisión, no un ajuste: se justifica en un
  [ADR](adr/) o se anota en [DUDAS_Y_CAMBIOS](DUDAS_Y_CAMBIOS.md).
- Cada escenario Gherkin de aquí debe acabar existiendo como test con un nombre reconocible
  ([ADR-0008](adr/0008-estrategia-de-pruebas.md)).
