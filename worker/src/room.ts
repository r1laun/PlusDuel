import {
  configForLevel,
  difficultyCoefficient,
  generateRound,
  roundScore,
  validateExpression,
  type WsServerMessage,
} from '@plusduel/shared';
import { type Env } from './env.js';

const BEST_OF = 5;
const ROUNDS_TO_WIN = Math.ceil(BEST_OF / 2);
const BETWEEN_ROUNDS_MS = 3500;
// Private rooms wait longer for a friend; quick-play reconnects should be instant.
const WAIT_QUICKPLAY_MS = 30_000;
const WAIT_PRIVATE_MS = 30 * 60_000;

interface RoomPlayer {
  token: string;
  name: string;
  score: number;
  roundWins: number;
}

interface RoundData {
  digits: number[];
  target: number;
  solution: string;
  timeLimitMs: number;
  startedAt: number;
}

type DeadlineKind = 'dissolve' | 'roundEnd' | 'nextRound';

interface PersistedRoom {
  code: string | null;
  kind: 'quickplay' | 'private';
  players: RoomPlayer[];
  phase: 'waiting' | 'round-active' | 'between-rounds' | 'ended';
  roundIndex: number;
  level: number;
  roundData: RoundData | null;
  deadline: { kind: DeadlineKind; at: number } | null;
  /** Per-token socket generation, bumped on every accept. */
  gens: Record<string, number>;
}

interface SocketMeta {
  token: string;
  /** Socket generation: bumped on every accept, so a replaced socket's
      late close event can never forfeit the live one. */
  gen: number;
}

const ROOM_KEY = 'room';

/**
 * One match. Hibernatable WebSocket + single alarm for the next deadline
 * (waiting dissolve / round end / next round). All state is persisted on
 * every mutation so an evicted DO resumes correctly on wake.
 */
export class RoomDO {
  private state: DurableObjectState;
  private env: Env;

  constructor(state: DurableObjectState, env: Env) {
    this.state = state;
    this.env = env;
  }

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    if (request.headers.get('Upgrade')?.toLowerCase() === 'websocket') {
      return this.handleSocket(request);
    }
    if (request.method === 'POST' && url.pathname === '/internal/init') {
      const body = (await request.json()) as {
        players: { token: string; name: string }[];
        code: string | null;
        kind: 'quickplay' | 'private';
      };
      const existing = await this.state.storage.get<PersistedRoom>(ROOM_KEY);
      if (existing) return new Response('already initialized', { status: 409 });
      const data: PersistedRoom = {
        code: body.code,
        kind: body.kind,
        players: body.players.map((p) => ({ token: p.token, name: p.name, score: 0, roundWins: 0 })),
        phase: 'waiting',
        roundIndex: 0,
        level: 1,
        roundData: null,
        deadline: null,
        gens: {},
      };
      await this.setDeadline(data, 'dissolve', Date.now() + (body.kind === 'private' ? WAIT_PRIVATE_MS : WAIT_QUICKPLAY_MS));
      return Response.json({ ok: true });
    }
    if (request.method === 'POST' && url.pathname === '/internal/add-guest') {
      const { token, name } = (await request.json()) as { token: string; name: string };
      const data = await this.load();
      if (!data || data.players.length >= 2) return new Response('full', { status: 409 });
      data.players.push({ token, name, score: 0, roundWins: 0 });
      await this.save(data);
      await this.maybeStart(data);
      return Response.json({ ok: true });
    }
    return new Response('Not found', { status: 404 });
  }

  private async handleSocket(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const token = url.searchParams.get('token') ?? '';
    const data = await this.load();
    const player = data?.players.find((p) => p.token === token);
    if (!data || !player || data.phase === 'ended') {
      return new Response('unknown player', { status: 401 });
    }
    // Replace a stale socket for the same token (fast refresh race).
    for (const old of this.state.getWebSockets(token)) {
      try {
        old.close(4400, 'replaced');
      } catch {
        /* already gone */
      }
    }
    const pair = new WebSocketPair();
    const gen = (data.gens[token] ?? 0) + 1;
    data.gens[token] = gen;
    await this.save(data);
    this.state.acceptWebSocket(pair[1], [token]);
    pair[1].serializeAttachment({ token, gen } satisfies SocketMeta);

    if (data.code && data.players.length === 1 && player === data.players[0]) {
      this.sendTo(token, { t: 'room:code', code: data.code });
    }
    await this.maybeStart(data);
    return new Response(null, { status: 101, webSocket: pair[0] });
  }

  async webSocketMessage(ws: WebSocket, message: string | ArrayBuffer): Promise<void> {
    if (typeof message !== 'string') return;
    const meta = ws.deserializeAttachment() as SocketMeta | null;
    if (!meta) return;
    let parsed: unknown;
    try {
      parsed = JSON.parse(message);
    } catch {
      return;
    }
    if (typeof parsed !== 'object' || parsed === null) return;
    const { t, expression } = parsed as { t?: unknown; expression?: unknown };
    if (t !== 'round:submit' || typeof expression !== 'string') return;
    await this.doSubmit(meta.token, expression);
  }

  async webSocketClose(ws: WebSocket): Promise<void> {
    const meta = ws.deserializeAttachment() as SocketMeta | null;
    if (!meta) return;
    const data = await this.load();
    if (!data || data.phase === 'ended') return;
    // Ignore stale closes from replaced sockets (generation moved on).
    if ((data.gens[meta.token] ?? 0) !== meta.gen) return;
    if (data.phase === 'waiting') {
      // Nobody to notify yet: dissolve only when everyone is gone,
      // otherwise the remaining party keeps waiting for the deadline.
      if (this.liveTokens().size === 0) await this.dissolve(data);
      return;
    }
    await this.forfeit(data, meta.token);
  }

  async webSocketError(ws: WebSocket): Promise<void> {
    await this.webSocketClose(ws);
  }

  /** Single alarm per DO: fires the pending deadline. */
  async alarm(): Promise<void> {
    const data = await this.load();
    if (!data || !data.deadline || data.phase === 'ended') return;
    // Commit point first: clear the deadline so a runtime retry of this
    // alarm becomes a no-op instead of a duplicate round/timeout.
    const dl = data.deadline;
    data.deadline = null;
    await this.save(data);
    if (data.phase === 'waiting') {
      if (!this.allPresent(data)) await this.dissolve(data);
      else await this.maybeStart(data);
      return;
    }
    if (data.phase === 'round-active' && dl.kind === 'roundEnd') {
      await this.doTimeout(data);
    } else if (data.phase === 'between-rounds' && dl.kind === 'nextRound') {
      await this.beginRound(data);
    }
  }

  // ---- match engine (ported from server/src/match.ts) ----

  private async maybeStart(data: PersistedRoom): Promise<void> {
    if (data.phase !== 'waiting' || data.players.length < 2 || !this.allPresent(data)) return;
    for (const p of data.players) {
      const other = data.players.find((o) => o.token !== p.token)!;
      this.sendTo(p.token, {
        t: 'match:start',
        roomCode: data.code,
        opponent: { id: other.token, name: other.name },
        youAre: p.token,
        bestOf: BEST_OF,
        roundsToWin: ROUNDS_TO_WIN,
      });
    }
    await this.beginRound(data);
  }

  private async beginRound(data: PersistedRoom): Promise<void> {
    if (data.phase === 'ended') return;
    data.phase = 'round-active';
    data.roundIndex++;
    data.level = Math.max(1, Math.min(6, data.roundIndex));

    const gen = generateRound(data.level);
    const cfg = configForLevel(data.level);
    const timeLimitMs = cfg.timeLimitSec * 1000;
    const coefficient = difficultyCoefficient(gen.digits.length, cfg.targetMax);
    const startedAt = Date.now();
    data.roundData = {
      digits: gen.digits,
      target: gen.target,
      solution: gen.solution,
      timeLimitMs,
      startedAt,
    };
    for (const p of data.players) {
      this.sendTo(p.token, {
        t: 'round:start',
        index: data.roundIndex,
        digits: gen.digits,
        target: gen.target,
        timeLimitMs,
        endsAt: startedAt + timeLimitMs,
        difficultyCoefficient: coefficient,
      });
    }
    await this.setDeadline(data, 'roundEnd', startedAt + timeLimitMs);
  }

  private async doSubmit(token: string, expression: string): Promise<void> {
    const data = await this.load();
    if (!data || data.phase !== 'round-active' || !data.roundData) return;
    const side = data.players.findIndex((p) => p.token === token);
    if (side === -1) return;
    const result = validateExpression(expression, data.roundData.digits, data.roundData.target);
    if (!result.valid) {
      this.sendTo(token, { t: 'game:error', message: `Invalid expression: ${result.reason}` });
      return;
    }
    const remainingMs = Math.max(0, data.roundData.startedAt + data.roundData.timeLimitMs - Date.now());
    const points = roundScore(
      data.roundData.digits.length,
      configForLevel(data.level).targetMax,
      remainingMs,
      data.roundData.timeLimitMs,
    );
    data.players[side]!.score += points;
    data.players[side]!.roundWins += 1;
    data.phase = 'between-rounds';

    for (const p of data.players) {
      this.sendTo(p.token, {
        t: 'round:end',
        index: data.roundIndex,
        winnerId: token,
        winningExpression: expression,
        sampleSolution: data.roundData!.solution,
        scoresByPlayer: this.scores(data),
        winsByPlayer: this.wins(data),
      });
    }
    if (this.matchOver(data)) {
      await this.finish(data);
      return;
    }
    await this.setDeadline(data, 'nextRound', Date.now() + BETWEEN_ROUNDS_MS);
  }

  private async doTimeout(data: PersistedRoom): Promise<void> {
    if (data.phase !== 'round-active') return;
    data.phase = 'between-rounds';
    for (const p of data.players) {
      this.sendTo(p.token, {
        t: 'round:end',
        index: data.roundIndex,
        winnerId: null,
        winningExpression: null,
        sampleSolution: data.roundData?.solution ?? null,
        scoresByPlayer: this.scores(data),
        winsByPlayer: this.wins(data),
      });
    }
    if (this.matchOver(data)) {
      await this.finish(data);
      return;
    }
    await this.setDeadline(data, 'nextRound', Date.now() + BETWEEN_ROUNDS_MS);
  }

  /** Ends at 3 round wins — or after 5 rounds (draw if tied). */
  private matchOver(data: PersistedRoom): boolean {
    return (
      data.players[0]!.roundWins >= ROUNDS_TO_WIN ||
      data.players[1]!.roundWins >= ROUNDS_TO_WIN ||
      data.roundIndex >= BEST_OF
    );
  }

  private async finish(data: PersistedRoom): Promise<void> {
    const [a, b] = data.players;
    const winner = !a || !b || a.roundWins === b.roundWins ? null : a.roundWins > b.roundWins ? a : b;
    for (const p of data.players) {
      this.sendTo(p.token, {
        t: 'match:end',
        winnerId: winner ? winner.token : null,
        reason: 'rounds',
        scoresByPlayer: this.scores(data),
        winsByPlayer: this.wins(data),
      });
    }
    await this.releaseCode(data);
    data.phase = 'ended';
    data.deadline = null;
    await this.state.storage.deleteAll();
  }

  private async forfeit(data: PersistedRoom, quitterToken: string): Promise<void> {
    const other = data.players.find((p) => p.token !== quitterToken);
    for (const p of data.players) {
      if (p.token === quitterToken) continue;
      this.sendTo(p.token, {
        t: 'match:end',
        winnerId: other ? other.token : null,
        reason: 'forfeit',
        scoresByPlayer: this.scores(data),
        winsByPlayer: this.wins(data),
      });
    }
    await this.releaseCode(data);
    await this.state.storage.deleteAll();
    data.phase = 'ended';
  }

  private async dissolve(data: PersistedRoom): Promise<void> {
    for (const ws of this.state.getWebSockets()) {
      try {
        ws.close(1000, 'dissolved');
      } catch {
        /* already gone */
      }
    }
    await this.releaseCode(data);
    await this.state.storage.deleteAll();
    data.phase = 'ended';
  }

  private async releaseCode(data: PersistedRoom): Promise<void> {
    if (!data.code) return;
    try {
      await this.env.LOBBY.get(this.env.LOBBY.idFromName('v1')).fetch(
        'http://lobby/internal/release-room',
        {
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ code: data.code }),
        },
      );
    } catch {
      /* lobby unreachable — registry entry expires on next resolve path */
    }
  }

  private allPresent(data: PersistedRoom): boolean {
    const live = this.liveTokens();
    return data.players.every((p) => live.has(p.token));
  }

  private liveTokens(): Set<string> {
    const live = new Set<string>();
    for (const ws of this.state.getWebSockets()) {
      const meta = ws.deserializeAttachment() as SocketMeta | null;
      if (meta) live.add(meta.token);
    }
    return live;
  }

  private sendTo(token: string, msg: WsServerMessage): void {
    for (const ws of this.state.getWebSockets(token)) {
      try {
        ws.send(JSON.stringify(msg));
      } catch {
        /* closing */
      }
    }
  }

  private scores(data: PersistedRoom): Record<string, number> {
    const out: Record<string, number> = {};
    for (const p of data.players) out[p.token] = p.score;
    return out;
  }

  private wins(data: PersistedRoom): Record<string, number> {
    const out: Record<string, number> = {};
    for (const p of data.players) out[p.token] = p.roundWins;
    return out;
  }

  private async setDeadline(
    data: PersistedRoom,
    kind: 'dissolve' | 'roundEnd' | 'nextRound',
    at: number,
  ): Promise<void> {
    data.deadline = { kind, at };
    await this.save(data);
    await this.state.storage.setAlarm(at);
  }

  private async load(): Promise<PersistedRoom | null> {
    return (await this.state.storage.get<PersistedRoom>(ROOM_KEY)) ?? null;
  }

  private async save(data: PersistedRoom): Promise<void> {
    await this.state.storage.put(ROOM_KEY, data);
  }
}
