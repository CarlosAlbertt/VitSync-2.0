# Conectar Jira y montar el tablero — VitSync 2.0

Objetivo: gestionar el proyecto como en una empresa (épicas, historias con criterios de
aceptación, estimación, sprints) y dejar rastro visible para el portfolio.

Tres rutas. La A y la B se complementan; la C es la alternativa sin Jira.

---

## Ruta A — MCP oficial de Atlassian (recomendada)

Permite que un asistente (Claude Code) lea y escriba en tu Jira por OAuth, sin manejar
tokens en ficheros.

**Requisito**: Jira **Cloud**. El plan gratuito (hasta 10 usuarios) es suficiente.
No funciona con Jira Server/Data Center.

### 1. Crear el proyecto

1. Alta en `https://www.atlassian.com/software/jira` → crea tu site (`tusitio.atlassian.net`).
2. Nuevo proyecto → plantilla **Scrum** → *Team-managed*.
3. Nombre `VitSync 2.0`, clave de proyecto **`VIT`**.
4. Activa el **Backlog** y los **Sprints** en la configuración de funcionalidades del proyecto.

### 2. Conectar el MCP

En una terminal (comando de la CLI de Claude Code, no del chat):

```bash
claude mcp add --transport http atlassian https://mcp.atlassian.com/v1/mcp/authv2
```

Se abre el navegador para autorizar por OAuth 2.1. Endpoint SSE heredado
(`https://mcp.atlassian.com/v1/sse`) sigue funcionando, pero está desaconsejado.

Comprobar que quedó registrado:

```bash
claude mcp list
```

### 3. Verificar

Reinicia la sesión y pide: *"lista mis proyectos de Jira"*. Si responde con `VIT`, listo.

### Límite conocido

Las herramientas del MCP cubren bien **issues** y búsquedas JQL. La gestión de **sprints**
(crear, rellenar, cerrar) pertenece a la Agile REST API y puede no estar expuesta. Para eso
está la Ruta B.

Referencias:
[repo oficial](https://github.com/atlassian/atlassian-mcp-server) ·
[documentación de Atlassian](https://support.atlassian.com/atlassian-rovo-mcp-server/docs/getting-started-with-the-atlassian-remote-mcp-server/)

---

## Ruta B — Bootstrap por REST API (cubre sprints)

Crea de una pasada las 9 épicas, todas las historias y los sprints S0–S8 desde
`scripts/jira/backlog.json`.

### 1. Token de API

Genera uno en `https://id.atlassian.com/manage-profile/security/api-tokens`.
**No lo escribas en ningún fichero del repo.** Se pasa por variable de entorno.

### 2. Variables de entorno

PowerShell:

```powershell
$env:JIRA_SITE="tusitio.atlassian.net"; $env:JIRA_EMAIL="tu@email.com"; $env:JIRA_API_TOKEN="..."; $env:JIRA_PROJECT_KEY="VIT"
```

Bash:

```bash
export JIRA_SITE=tusitio.atlassian.net JIRA_EMAIL=tu@email.com JIRA_API_TOKEN=... JIRA_PROJECT_KEY=VIT
```

### 3. Ensayo en seco (no escribe nada)

```bash
node scripts/jira/bootstrap.mjs --dry-run
```

### 4. Ejecución real

```bash
node scripts/jira/bootstrap.mjs
```

El script es **idempotente por resumen**: antes de crear una historia comprueba por JQL si
ya existe una con el mismo `summary` en el proyecto, y la omite. Se puede relanzar sin
duplicar. Requiere Node ≥ 20.

### 5. Sprints

Los sprints necesitan el `boardId` del tablero Scrum (aparece en la URL del tablero:
`.../boards/1`). Pásalo así:

```bash
node scripts/jira/bootstrap.mjs --board 1 --with-sprints
```

Si tu proyecto es *team-managed* y la API de sprints te devuelve 403, crea los 9 sprints a
mano desde el Backlog (30 segundos) y relanza con `--assign-sprints`.

---

## Ruta C — Sin Jira: GitHub Projects

Gratis, sin OAuth y pegado al código. Con la CLI de GitHub:

```bash
gh project create --owner CarlosAlbertt --title "VitSync 2.0"
```

Columnas: `Backlog → Ready → In progress → In review → Done`. Los issues se enlazan solos
con los PRs que los cierran (`Closes #12`), que es justo lo que un revisor quiere ver.

---

## Estructura del backlog

Definida en [`scripts/jira/backlog.json`](../scripts/jira/backlog.json).

### Épicas (una por sprint)

| Épica | Sprint | Objetivo |
|---|---|---|
| `Kickoff e infraestructura` | S0 | ADRs, docker compose, Flyway, CI |
| `Autenticación y cuentas` | S1 | Registro, verificación, login, refresh rotativo, 2FA |
| `Catálogo` | S2 | Especialidades, centros, cuadro médico |
| `Reserva de citas` | S3 | Agenda, huecos, reservar, cancelar |
| `Perfil y datos clínicos` | S4 | Perfil, cifrado en reposo, sesiones activas |
| `Informes y ficheros` | S5 | S3/MinIO, presigned URLs, descarga auditada |
| `Panel de administración` | S6 | CRUD usuarios, médicos, especialidades |
| `RGPD` | S7 | Consentimientos, exportación, supresión |
| `Calidad y salida a producción` | S8 | E2E, observabilidad, despliegue, demo |

### Convenciones de los issues

- **Historia**: `Como <rol> quiero <acción> para <beneficio>`.
- **Criterios de aceptación**: en Gherkin, dentro de la descripción.
- **Estimación**: Fibonacci (1, 2, 3, 5, 8). Nada por encima de 8: si sale, se parte.
- **Etiquetas**: `backend`, `frontend`, `db`, `security`, `gdpr`, `infra`, `docs`.
- **Definition of Done**: checklist en cada issue (guía, Anexo A).
- **Rama**: `feat/VIT-12-slug`. **Commit**: `feat(appointments): add cancel endpoint (VIT-12)`.

### Ritmo

Sprints de una semana. Al cerrar cada uno: demo de lo hecho, actualizar
[`HANDOFF.md`](HANDOFF.md) y anotar en el propio sprint qué se quedó fuera y por qué.
Ese registro es la parte que casi nadie lleva en un portfolio y la que más dice de ti.
