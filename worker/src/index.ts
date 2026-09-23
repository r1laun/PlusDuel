import { LobbyDO } from './lobby.js';
import { RoomDO } from './room.js';
import { type Env, json, sanitizeName } from './env.js';

export { LobbyDO, RoomDO };

function lobbyStub(env: Env): DurableObjectStub {
  return env.LOBBY.get(env.LOBBY.idFromName('v1'));
}

async function lobbyCall(env: Env, path: string, body: unknown): Promise<Response> {
  return lobbyStub(env).fetch(`http://lobby${path}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
}

/** Forward a client WS upgrade to a DO, injecting routing params. */
function forwardUpgrade(request: Request, stub: DurableObjectStub, params: Record<string, string>): Response | Promise<Response> {
  const url = new URL(request.url);
  for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);
  return stub.fetch(new Request(url.toString(), request));
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === '/health') {
      return new Response('ok', { headers: { 'content-type': 'text/plain' } });
    }

    if (!url.pathname.startsWith('/ws/')) {
      return new Response('Not found', { status: 404 });
    }
    if (request.headers.get('Upgrade')?.toLowerCase() !== 'websocket') {
      return new Response('WebSocket upgrade required', { status: 400 });
    }

    // Quick play → lobby queue.
    if (url.pathname === '/ws/quickplay') {
      const name = sanitizeName(url.searchParams.get('name'));
      const fwd = new URL(request.url);
      fwd.searchParams.set('name', name);
      return lobbyStub(env).fetch(new Request(fwd.toString(), request));
    }

    // Private room creation → lobby mints code + room, host socket goes to the room.
    if (url.pathname === '/ws/room/new') {
      const name = sanitizeName(url.searchParams.get('name'));
      const hostToken = crypto.randomUUID();
      const res = await lobbyCall(env, '/internal/create-room', { hostToken, hostName: name });
      if (!res.ok) return new Response('Could not create room', { status: 500 });
      const { roomId, code } = (await res.json()) as { roomId: string; code: string };
      const stub = env.ROOM.get(env.ROOM.idFromString(roomId));
      return forwardUpgrade(request, stub, { token: hostToken, name, code });
    }

    // Quick-play handover: room already initialized with both players,
    // no lobby lookup needed — the roomId is unguessable by itself.
    const idMatch = /^\/ws\/room\/id\/([0-9a-f]{64})$/.exec(url.pathname);
    if (idMatch) {
      const name = sanitizeName(url.searchParams.get('name'));
      const token = url.searchParams.get('token') ?? '';
      const stub = env.ROOM.get(env.ROOM.idFromString(idMatch[1]!));
      return forwardUpgrade(request, stub, { token, name });
    }

    // Private room join by code.
    const joinMatch = /^\/ws\/room\/([A-Za-z0-9]{4,8})$/.exec(url.pathname);
    if (joinMatch) {
      const name = sanitizeName(url.searchParams.get('name'));
      const guestToken = crypto.randomUUID();
      const res = await lobbyCall(env, '/internal/resolve-room', {
        code: joinMatch[1]!,
        guestToken,
        guestName: name,
      });
      if (res.status === 404) return new Response('Room not found', { status: 404 });
      if (!res.ok) return new Response('Room already full', { status: 410 });
      const { roomId } = (await res.json()) as { roomId: string };
      const stub = env.ROOM.get(env.ROOM.idFromString(roomId));
      return forwardUpgrade(request, stub, { token: guestToken, name });
    }

    return new Response('Not found', { status: 404 });
  },
};
