import type { WsServerMessage } from '@plusduel/shared';
import { type Env, json, sanitizeName } from './env.js';

interface QueueEntry {
  token: string;
  name: string;
}

interface RoomRecord {
  roomId: string;
  hostToken: string;
  guestToken: string | null;
}

interface LobbyData {
  queue: QueueEntry[];
  /** private-room code (uppercased) -> record */
  rooms: Record<string, RoomRecord>;
}

interface SocketMeta {
  token: string;
  name: string;
}

const LOBBY_KEY = 'lobby';

/**
 * Singleton matchmaking DO: FIFO quick-play queue + private-room code registry.
 * Quick-play sockets live here until paired; paired players reconnect to a RoomDO.
 */
export class LobbyDO {
  private state: DurableObjectState;
  private env: Env;

  constructor(state: DurableObjectState, env: Env) {
    this.state = state;
    this.env = env;
  }

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    if (request.headers.get('Upgrade')?.toLowerCase() === 'websocket') {
      return this.handleQueueSocket(request);
    }
    if (request.method === 'POST' && url.pathname === '/internal/create-room') {
      const { hostToken, hostName } = (await request.json()) as {
        hostToken: string;
        hostName: string;
      };
      return json(await this.createPrivateRoom(hostToken, hostName));
    }
    if (request.method === 'POST' && url.pathname === '/internal/resolve-room') {
      const { code, guestToken, guestName } = (await request.json()) as {
        code: string;
        guestToken: string;
        guestName: string;
      };
      const res = await this.resolveJoin(code, guestToken, guestName);
      if (!res) return json({ error: 'not-found' }, 404);
      if ('error' in res) return json(res, 410);
      return json(res);
    }
    if (request.method === 'POST' && url.pathname === '/internal/release-room') {
      const { code } = (await request.json()) as { code: string };
      const data = await this.load();
      delete data.rooms[code.trim().toUpperCase()];
      await this.save(data);
      return json({ ok: true });
    }
    return new Response('Not found', { status: 404 });
  }

  private async handleQueueSocket(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const name = sanitizeName(url.searchParams.get('name'));
    const token = crypto.randomUUID();
    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];
    this.state.acceptWebSocket(server, [token]);
    server.serializeAttachment({ token, name } satisfies SocketMeta);

    const data = await this.load();
    this.pruneQueue(data);
    data.queue.push({ token, name });
    await this.save(data);
    this.sendTo(token, { t: 'game:queued', position: data.queue.length });
    await this.tryPair(data);
    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketClose(ws: WebSocket): Promise<void> {
    const meta = ws.deserializeAttachment() as SocketMeta | null;
    if (!meta) return;
    const data = await this.load();
    const before = data.queue.length;
    data.queue = data.queue.filter((e) => e.token !== meta.token);
    if (data.queue.length !== before) await this.save(data);
  }

  async webSocketError(ws: WebSocket): Promise<void> {
    await this.webSocketClose(ws);
  }

  /** Pair waiting players into fresh RoomDOs. Clients reconnect via match:found. */
  private async tryPair(data: LobbyData): Promise<void> {
    this.pruneQueue(data);
    while (data.queue.length >= 2) {
      const a = data.queue.shift()!;
      const b = data.queue.shift()!;
      const roomId = this.env.ROOM.newUniqueId();
      try {
        await this.initRoom(roomId.toString(), {
          players: [
            { token: a.token, name: a.name },
            { token: b.token, name: b.name },
          ],
          code: null,
          kind: 'quickplay',
        });
        this.sendTo(a.token, {
          t: 'match:found',
          roomId: roomId.toString(),
          token: a.token,
          opponentName: b.name,
        });
        this.sendTo(b.token, {
          t: 'match:found',
          roomId: roomId.toString(),
          token: b.token,
          opponentName: a.name,
        });
      } catch (e) {
        // Room init failed — put the players back at the head of the queue.
        console.error('[lobby] pair failed:', e);
        data.queue.unshift(b, a);
        break;
      }
    }
    await this.save(data);
  }

  private async createPrivateRoom(
    hostToken: string,
    hostName: string,
  ): Promise<{ roomId: string; code: string; token: string }> {
    const data = await this.load();
    let code = '';
    for (let i = 0; i < 20 && !code; i++) {
      const candidate = randomCode();
      if (!data.rooms[candidate]) code = candidate;
    }
    if (!code) throw new Error('no room code available');
    const roomId = this.env.ROOM.newUniqueId().toString();
    await this.initRoom(roomId, {
      players: [{ token: hostToken, name: sanitizeName(hostName) }],
      code,
      kind: 'private',
    });
    data.rooms[code] = { roomId, hostToken, guestToken: null };
    await this.save(data);
    return { roomId, code, token: hostToken };
  }

  private async resolveJoin(
    rawCode: string,
    guestToken: string,
    guestName: string,
  ): Promise<{ roomId: string; token: string } | { error: string } | null> {
    const code = rawCode.trim().toUpperCase();
    const data = await this.load();
    const rec = data.rooms[code];
    if (!rec) return null;
    if (rec.guestToken && rec.guestToken !== guestToken) return { error: 'full' };
    rec.guestToken = guestToken;
    await this.save(data);
    const addRes = await this.env.ROOM.get(this.env.ROOM.idFromString(rec.roomId)).fetch(
      'http://room/internal/add-guest',
      {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ token: guestToken, name: sanitizeName(guestName) }),
      },
    );
    if (!addRes.ok) {
      const fresh = await this.load();
      delete fresh.rooms[code];
      await this.save(fresh);
      return null;
    }
    return { roomId: rec.roomId, token: guestToken };
  }

  private async initRoom(
    roomId: string,
    body: { players: { token: string; name: string }[]; code: string | null; kind: string },
  ): Promise<void> {
    const res = await this.env.ROOM.get(this.env.ROOM.idFromString(roomId)).fetch(
      'http://room/internal/init',
      {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(body),
      },
    );
    if (!res.ok) throw new Error(`room init failed: ${res.status}`);
  }

  /** Drop queue entries whose sockets are gone (e.g. after hibernation wake). */
  private pruneQueue(data: LobbyData): void {
    const live = new Set<string>();
    for (const ws of this.state.getWebSockets()) {
      const meta = ws.deserializeAttachment() as SocketMeta | null;
      if (meta) live.add(meta.token);
    }
    data.queue = data.queue.filter((e) => live.has(e.token));
  }

  private sendTo(token: string, msg: WsServerMessage): void {
    for (const ws of this.state.getWebSockets(token)) {
      try {
        ws.send(JSON.stringify(msg));
      } catch {
        /* closing — pruned on close event */
      }
    }
  }

  private async load(): Promise<LobbyData> {
    const data = await this.state.storage.get<LobbyData>(LOBBY_KEY);
    return data ?? { queue: [], rooms: {} };
  }

  private async save(data: LobbyData): Promise<void> {
    await this.state.storage.put(LOBBY_KEY, data);
  }
}

function randomCode(): string {
  const alphabet = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
  let code = '';
  const bytes = crypto.getRandomValues(new Uint8Array(4));
  for (const byte of bytes) code += alphabet[byte % alphabet.length]!;
  return code;
}
