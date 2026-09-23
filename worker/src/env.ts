export interface Env {
  LOBBY: DurableObjectNamespace;
  ROOM: DurableObjectNamespace;
}

export function sanitizeName(name: unknown): string {
  if (typeof name !== 'string') return `Guest${Math.floor(Math.random() * 900 + 100)}`;
  const trimmed = name.trim().slice(0, 20);
  return trimmed.length >= 2 ? trimmed : `Guest${Math.floor(Math.random() * 900 + 100)}`;
}

export function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}
