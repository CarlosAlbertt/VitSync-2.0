import { env } from "@/shared/config/env";
import { useSession } from "./session";
import type { Problem, Session } from "./types";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly traceId?: string;
  readonly fieldErrors: { field: string; message: string }[];

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title ?? "Error");
    this.status = problem.status;
    this.code = problem.code ?? "UNKNOWN";
    this.traceId = problem.traceId;
    this.fieldErrors = problem.errors ?? [];
  }
}

type Options = { method?: string; body?: unknown; query?: Record<string, string | number | undefined> };

function buildUrl(path: string, query?: Options["query"]) {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  const search = params.toString();
  return `${env.VITE_API_URL}/api/v1${path}${search ? `?${search}` : ""}`;
}

function send(path: string, { method = "GET", body, query }: Options) {
  const token = useSession.getState().accessToken;
  return fetch(buildUrl(path, query), {
    method,
    credentials: "include", // cookie httpOnly del refresh
    headers: {
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

async function parse<T>(response: Response): Promise<T> {
  if (response.ok) {
    // 201, 202 y 204 pueden llegar sin cuerpo.
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }
  const problem = (await response.json().catch(() => ({}))) as Partial<Problem>;
  throw new ApiError({ ...problem, status: response.status });
}

// Single-flight: si varias peticiones reciben 401 a la vez, solo sale un refresh.
let refreshing: Promise<boolean> | null = null;

export function refreshSession(): Promise<boolean> {
  refreshing ??= send("/auth/refresh", { method: "POST" })
    .then(parse<Session>)
    .then((session) => {
      useSession.getState().signIn(session);
      return true;
    })
    .catch(() => {
      useSession.getState().signOut();
      return false;
    })
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

export async function request<T>(path: string, options: Options = {}): Promise<T> {
  const response = await send(path, options);
  const retriable = response.status === 401 && !path.startsWith("/auth/");
  if (retriable && (await refreshSession())) {
    return parse<T>(await send(path, options));
  }
  return parse<T>(response);
}
