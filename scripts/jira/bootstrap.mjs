#!/usr/bin/env node
/**
 * Bootstrap del backlog de VitSync 2.0 en Jira Cloud.
 *
 * Crea las épicas, las historias (con criterios de aceptación en Gherkin,
 * estimación y etiquetas) y, opcionalmente, los sprints del tablero Scrum.
 *
 * Uso:
 *   node scripts/jira/bootstrap.mjs --dry-run
 *   node scripts/jira/bootstrap.mjs
 *   node scripts/jira/bootstrap.mjs --board 1 --with-sprints
 *   node scripts/jira/bootstrap.mjs --board 1 --assign-sprints
 *
 * Variables de entorno (ninguna se escribe en disco):
 *   JIRA_SITE          tusitio.atlassian.net
 *   JIRA_EMAIL         email de la cuenta Atlassian
 *   JIRA_API_TOKEN     token de https://id.atlassian.com/manage-profile/security/api-tokens
 *   JIRA_PROJECT_KEY   clave del proyecto (por defecto, la del backlog.json)
 *   JIRA_POINTS_FIELD  opcional; id del campo de estimación (p. ej. customfield_10016)
 *
 * Es idempotente por resumen: antes de crear una issue comprueba por JQL si ya
 * existe una con el mismo summary en el proyecto. Se puede relanzar sin duplicar.
 *
 * Requiere Node >= 20 (usa fetch global).
 */

import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const HERE = dirname(fileURLToPath(import.meta.url));

// ─── argumentos ──────────────────────────────────────────────────────────────
const argv = process.argv.slice(2);
const flag = (name) => argv.includes(`--${name}`);
const value = (name, fallback = null) => {
  const i = argv.indexOf(`--${name}`);
  return i >= 0 && argv[i + 1] ? argv[i + 1] : fallback;
};

const DRY_RUN = flag("dry-run");
const WITH_SPRINTS = flag("with-sprints");
const ASSIGN_SPRINTS = flag("assign-sprints") || WITH_SPRINTS;
const BOARD_ID = value("board");
const BACKLOG_PATH = resolve(HERE, value("backlog", "backlog.json"));

// ─── configuración ───────────────────────────────────────────────────────────
const { JIRA_SITE, JIRA_EMAIL, JIRA_API_TOKEN, JIRA_POINTS_FIELD } = process.env;

if (!DRY_RUN && (!JIRA_SITE || !JIRA_EMAIL || !JIRA_API_TOKEN)) {
  console.error(
    "Faltan variables de entorno: JIRA_SITE, JIRA_EMAIL, JIRA_API_TOKEN.\n" +
      "Prueba primero con --dry-run para ver qué se crearía."
  );
  process.exit(1);
}

const AUTH = "Basic " + Buffer.from(`${JIRA_EMAIL}:${JIRA_API_TOKEN}`).toString("base64");
const BASE = `https://${JIRA_SITE}`;

// ─── cliente HTTP ────────────────────────────────────────────────────────────
async function call(path, { method = "GET", body } = {}) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      Authorization: AUTH,
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await res.text();
  const data = text ? safeJson(text) : null;

  if (!res.ok) {
    const detail = data?.errorMessages?.join(" · ") || data?.errors || text.slice(0, 300);
    const err = new Error(`${method} ${path} → ${res.status}: ${detail}`);
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

const safeJson = (t) => {
  try {
    return JSON.parse(t);
  } catch {
    return null;
  }
};

// ─── Atlassian Document Format ───────────────────────────────────────────────
const paragraph = (text) => ({
  type: "paragraph",
  content: [{ type: "text", text }],
});

const codeBlock = (text) => ({
  type: "codeBlock",
  attrs: { language: "gherkin" },
  content: [{ type: "text", text }],
});

const heading = (text) => ({
  type: "heading",
  attrs: { level: 3 },
  content: [{ type: "text", text }],
});

const DOD = [
  "Tests unitarios y de integración en verde",
  "Migración Flyway incluida si toca el esquema",
  "Test de autorización (usuario ajeno → 403)",
  "OpenAPI actualizado sin drift",
  "Sin secretos, sin TODO huérfanos, sin código temporal",
  "README o ADR actualizado si cambia una decisión",
];

const taskList = (items) => ({
  type: "taskList",
  attrs: { localId: "dod" },
  content: items.map((text, i) => ({
    type: "taskItem",
    attrs: { localId: `dod-${i}`, state: "TODO" },
    content: [{ type: "text", text }],
  })),
});

function storyDescription(story) {
  const gherkin = story.ac
    .map((line, i) => (i === 0 ? `  ${line}` : `  ${line}`))
    .join("\n");

  return {
    type: "doc",
    version: 1,
    content: [
      paragraph(story.as),
      heading("Criterios de aceptación"),
      codeBlock(`Escenario: ${story.summary}\n${gherkin}`),
      heading("Definition of Done"),
      taskList(DOD),
    ],
  };
}

const plainDescription = (text) => ({
  type: "doc",
  version: 1,
  content: [paragraph(text)],
});

// ─── búsqueda idempotente ────────────────────────────────────────────────────
const escapeJql = (s) => s.replace(/["\\]/g, "\\$&");

async function findBySummary(projectKey, summary) {
  const jql = `project = "${projectKey}" AND summary ~ "\\"${escapeJql(summary)}\\""`;
  try {
    // endpoint actual de Jira Cloud
    const data = await call("/rest/api/3/search/jql", {
      method: "POST",
      body: { jql, fields: ["summary"], maxResults: 5 },
    });
    return matchExact(data?.issues, summary);
  } catch (e) {
    if (e.status !== 404 && e.status !== 410) throw e;
    // endpoint heredado
    const data = await call(
      `/rest/api/3/search?jql=${encodeURIComponent(jql)}&fields=summary&maxResults=5`
    );
    return matchExact(data?.issues, summary);
  }
}

const matchExact = (issues, summary) =>
  (issues || []).find((i) => i.fields?.summary === summary) || null;

// ─── creación de issues ──────────────────────────────────────────────────────
async function createIssue({ projectKey, type, summary, description, labels, points, parentKey }) {
  const fields = {
    project: { key: projectKey },
    issuetype: { name: type },
    summary,
    description,
    labels,
  };
  if (parentKey) fields.parent = { key: parentKey };
  if (points && JIRA_POINTS_FIELD) fields[JIRA_POINTS_FIELD] = points;

  try {
    return await call("/rest/api/3/issue", { method: "POST", body: { fields } });
  } catch (e) {
    // el campo de estimación varía entre sites: reintenta sin él antes de rendirse
    if (e.status === 400 && JIRA_POINTS_FIELD && fields[JIRA_POINTS_FIELD] !== undefined) {
      delete fields[JIRA_POINTS_FIELD];
      console.warn(`   ! campo de puntos rechazado, se crea sin estimación: ${summary}`);
      return await call("/rest/api/3/issue", { method: "POST", body: { fields } });
    }
    throw e;
  }
}

// ─── sprints (Agile API) ─────────────────────────────────────────────────────
async function ensureSprint(boardId, name, goal) {
  const existing = await call(`/rest/agile/1.0/board/${boardId}/sprint?state=future,active`);
  const found = (existing?.values || []).find((s) => s.name === name);
  if (found) return found;
  return call("/rest/agile/1.0/sprint", {
    method: "POST",
    body: { name, goal, originBoardId: Number(boardId) },
  });
}

const moveIssuesToSprint = (sprintId, issueKeys) =>
  call(`/rest/agile/1.0/sprint/${sprintId}/issue`, {
    method: "POST",
    body: { issues: issueKeys },
  });

// ─── ejecución ───────────────────────────────────────────────────────────────
async function main() {
  const backlog = JSON.parse(await readFile(BACKLOG_PATH, "utf8"));
  const projectKey = process.env.JIRA_PROJECT_KEY || backlog.project;

  const totalStories = backlog.epics.reduce((n, e) => n + e.stories.length, 0);
  const totalPoints = backlog.epics
    .flatMap((e) => e.stories)
    .reduce((n, s) => n + (s.points || 0), 0);

  console.log(`Proyecto: ${projectKey}`);
  console.log(`Épicas: ${backlog.epics.length} · Historias: ${totalStories} · Puntos: ${totalPoints}`);
  if (DRY_RUN) console.log("MODO ENSAYO: no se escribe nada en Jira.\n");

  const bySprint = new Map(); // sprintId → [issueKey]

  for (const epic of backlog.epics) {
    let epicKey = `${projectKey}-DRY`;

    if (DRY_RUN) {
      console.log(`\n[épica] ${epic.summary}`);
    } else {
      const existing = await findBySummary(projectKey, epic.summary);
      if (existing) {
        epicKey = existing.key;
        console.log(`\n[épica] ${epic.summary} → ya existe (${epicKey})`);
      } else {
        const created = await createIssue({
          projectKey,
          type: "Epic",
          summary: epic.summary,
          description: plainDescription(epic.description),
          labels: epic.labels,
        });
        epicKey = created.key;
        console.log(`\n[épica] ${epic.summary} → creada (${epicKey})`);
      }
    }

    for (const story of epic.stories) {
      if (DRY_RUN) {
        console.log(`   · ${story.summary}  [${story.points} pts, ${story.sprint}]`);
        continue;
      }

      const existing = await findBySummary(projectKey, story.summary);
      let key;
      if (existing) {
        key = existing.key;
        console.log(`   = ${key} ${story.summary} (ya existía)`);
      } else {
        const created = await createIssue({
          projectKey,
          type: "Story",
          summary: story.summary,
          description: storyDescription(story),
          labels: story.labels,
          points: story.points,
          parentKey: epicKey,
        });
        key = created.key;
        console.log(`   + ${key} ${story.summary}`);
      }

      if (!bySprint.has(story.sprint)) bySprint.set(story.sprint, []);
      bySprint.get(story.sprint).push(key);
    }
  }

  if (!ASSIGN_SPRINTS) {
    console.log("\nListo. Para crear y rellenar los sprints: --board <id> --with-sprints");
    return;
  }

  if (!BOARD_ID) {
    console.error("\n--with-sprints/--assign-sprints requieren --board <id> (mira la URL del tablero).");
    process.exit(1);
  }

  console.log("\nSprints:");
  for (const sprint of backlog.sprints) {
    const keys = bySprint.get(sprint.id) || [];
    if (DRY_RUN) {
      console.log(`   · ${sprint.name} → ${keys.length || "?"} historias`);
      continue;
    }
    try {
      const created = await ensureSprint(BOARD_ID, sprint.name, sprint.goal);
      if (keys.length) await moveIssuesToSprint(created.id, keys);
      console.log(`   + ${sprint.name} (id ${created.id}) ← ${keys.length} historias`);
    } catch (e) {
      console.error(`   ! ${sprint.name}: ${e.message}`);
      console.error("     Si es 403, crea los sprints a mano en el Backlog y relanza con --assign-sprints.");
    }
  }
}

main().catch((e) => {
  console.error("\nError:", e.message);
  process.exit(1);
});
