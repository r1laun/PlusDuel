import {
  createPrivate,
  getSnapshot,
  joinPrivate,
  leave,
  quickplay,
  submit,
  type JoinError,
  type Snapshot,
} from '@plusduel/shared';
import { blobKV } from './store.js';

/**
 * Single Netlify Function serving the whole game API.
 * netlify.toml redirects /api/* here; the route is the path after /api/.
 *
 * Routes:
 *   POST /api/quickplay {name}              → {session, snapshot}
 *   POST /api/room/new {name}               → {session, snapshot, code}
 *   POST /api/room/join {name, code}        → {session, snapshot} (404/410)
 *   GET  /api/state?playerId=&token=        → snapshot (also a heartbeat)
 *   POST /api/submit {playerId,token,expr}  → {accepted, message?, snapshot}
 *   POST /api/leave {playerId,token}        → {ok:true}
 *   GET  /api/health                        → ok (no store access)
 */

interface LambdaEvent {
  httpMethod: string;
  path: string;
  queryStringParameters?: Record<string, string | undefined> | null;
  body?: string | null;
}

const json = (status: number, data: unknown): Response =>
  new Response(JSON.stringify(data), {
    status,
    headers: { 'content-type': 'application/json' },
  });

const bad = (status: number, message: string): Response => json(status, { message });

function routeOf(path: string): string[] {
  const segs = path.split('/').filter(Boolean);
  const apiIdx = segs.lastIndexOf('api');
  const rest = (apiIdx === -1 ? segs : segs.slice(apiIdx + 1)).filter((s) => s !== '.netlify' && s !== 'functions');
  return rest;
}

function bodyOf(event: LambdaEvent): Record<string, unknown> {
  if (!event.body) return {};
  try {
    return (JSON.parse(event.body) as Record<string, unknown>) ?? {};
  } catch {
    return {};
  }
}

export async function handler(event: LambdaEvent): Promise<Response> {
  const route = routeOf(event.path);
  const now = Date.now();

  if (event.httpMethod === 'GET' && route.length === 1 && route[0] === 'health') {
    return new Response('ok', { headers: { 'content-type': 'text/plain' } });
  }

  try {
    const kv = blobKV();

    if (event.httpMethod === 'POST' && route.length === 1 && route[0] === 'quickplay') {
      const { name } = bodyOf(event);
      const { playerId, token, snapshot } = await quickplay(kv, name, now);
      return json(200, { session: { playerId, token }, snapshot });
    }

    if (
      event.httpMethod === 'POST' &&
      route.length === 2 &&
      route[0] === 'room' &&
      route[1] === 'new'
    ) {
      const { name } = bodyOf(event);
      const { playerId, token, code, snapshot } = await createPrivate(kv, name, now);
      return json(200, { session: { playerId, token }, snapshot, code });
    }

    if (
      event.httpMethod === 'POST' &&
      route.length === 2 &&
      route[0] === 'room' &&
      route[1] === 'join'
    ) {
      const { name, code } = bodyOf(event);
      const res = await joinPrivate(kv, name, code, now);
      if ('error' in res) {
        const status: Record<JoinError, number> = { 'not-found': 404, full: 410, expired: 410 };
        return bad(status[res.error], res.error === 'not-found' ? 'Room not found' : 'Room unavailable');
      }
      return json(200, { session: { playerId: res.playerId, token: res.token }, snapshot: res.snapshot });
    }

    if (event.httpMethod === 'GET' && route.length === 1 && route[0] === 'state') {
      const q = event.queryStringParameters ?? {};
      const snapshot: Snapshot = await getSnapshot(kv, q.playerId ?? '', q.token ?? '', now);
      return json(200, { snapshot });
    }

    if (event.httpMethod === 'POST' && route.length === 1 && route[0] === 'submit') {
      const { playerId, token, expression } = bodyOf(event);
      const res = await submit(kv, String(playerId ?? ''), String(token ?? ''), expression, now);
      return json(200, res);
    }

    if (event.httpMethod === 'POST' && route.length === 1 && route[0] === 'leave') {
      const { playerId, token } = bodyOf(event);
      await leave(kv, String(playerId ?? ''), String(token ?? ''), now);
      return json(200, { ok: true });
    }

    return bad(404, 'Not found');
  } catch (err) {
    console.error('api handler failed', err);
    return bad(500, 'Internal error');
  }
}
