import { configForLevel, difficultyCoefficient, roundScore } from './difficulty.js';
import { generateRound } from './generator.js';
import { validateExpression } from './validator/index.js';
import type { JoinError, Snapshot } from './api.js';
import type { MatchEndPayload, MatchStartPayload, RoundEndPayload, RoundStartPayload } from './types.js';

/**
 * PlusDuel match engine, serverless edition.
 *
 * The original socket.io server held all state in memory and drove the game
 * with timers. Netlify Functions are stateless and short-lived, so the engine
 * is pure: every transition is a function of (persisted records, now).
 * Timeouts and between-round gaps resolve lazily whenever a room is touched
 * (poll or submit) instead of via setTimeout.
 *
 * Persistence is behind the KV interface — Netlify Blobs in production,
 * memoryKV() in tests and local tooling.
 */

export const BEST_OF = 5;
export const ROUNDS_TO_WIN = Math.ceil(BEST_OF / 2);
export const BETWEEN_ROUNDS_MS = 3500;
/** Queued player pruned when unseen this long (client polls every ~1s). */
export const QUEUE_TIMEOUT_MS = 15_000;
/** Mid-match opponent treated as forfeited when unseen this long. */
export const OPPONENT_TIMEOUT_MS = 20_000;
/** Private room host waits at most this long for a challenger. */
export const WAITING_TTL_MS = 10 * 60_000;

export interface KV {
  get<T>(key: string): Promise<T | null>;
  set(key: string, value: unknown): Promise<void>;
  del(key: string): Promise<void>;
}

/** In-memory KV for tests. Not for production (no cross-instance sharing). */
export function memoryKV(): KV {
  const m = new Map<string, unknown>();
  return {
    get: async <T>(key: string): Promise<T | null> =>
      (m.has(key) ? (m.get(key) as T) : null),
    set: async (key: string, value: unknown): Promise<void> => {
      // Deep-clone on write so tests can't alias stored records.
      m.set(key, JSON.parse(JSON.stringify(value)));
    },
    del: async (key: string): Promise<void> => {
      m.delete(key);
    },
  };
}

export interface EnginePlayer {
  id: string;
  name: string;
  token: string;
  score: number;
  roundWins: number;
  lastSeenAt: number;
}

export interface EngineRound {
  index: number;
  digits: number[];
  target: number;
  solution: string;
  timeLimitMs: number;
  startedAt: number;
  endsAt: number;
  difficultyCoefficient: number;
}

export type RoomPhase = 'waiting' | 'round-active' | 'between-rounds' | 'ended';

export interface EngineRoom {
  id: string;
  code: string | null;
  players: EnginePlayer[];
  phase: RoomPhase;
  roundIndex: number;
  level: number;
  round: EngineRound | null;
  roundEnd: RoundEndPayload | null;
  roundEndedAt: number | null;
  matchEnd: MatchEndPayload | null;
  createdAt: number;
  updatedAt: number;
}

export interface QueueEntry {
  playerId: string;
  name: string;
  token: string;
  enqueuedAt: number;
  lastSeenAt: number;
}

export interface SessionRec {
  token: string;
  roomId: string | null;
  queued: boolean;
}

export const sessKey = (playerId: string): string => `sess/${playerId}`;
export const roomKey = (roomId: string): string => `room/${roomId}`;
export const codeKey = (code: string): string => `code/${code}`;
export const QUEUE_KEY = 'queue';

const ADJECTIVES = ['Swift', 'Clever', 'Fierce', 'Lucid', 'Prime', 'Cosmic', 'Rapid', 'Sharp'];
const NOUNS = ['Otter', 'Falcon', 'Turing', 'Panda', 'Vector', 'Comet', 'Pixel', 'Nova'];

export function randomName(): string {
  const a = ADJECTIVES[Math.floor(Math.random() * ADJECTIVES.length)];
  const n = NOUNS[Math.floor(Math.random() * NOUNS.length)];
  return `${a}${n}${Math.floor(Math.random() * 90 + 10)}`;
}

export function sanitizeName(name: unknown): string | null {
  if (typeof name !== 'string') return null;
  const trimmed = name.trim().slice(0, 20);
  return trimmed.length >= 2 ? trimmed : null;
}

function newIds(): { playerId: string; token: string } {
  return { playerId: crypto.randomUUID(), token: crypto.randomUUID() };
}

function newCode(): string {
  return Math.random().toString(36).slice(2, 6).toUpperCase();
}

async function loadQueue(kv: KV): Promise<QueueEntry[]> {
  return (await kv.get<QueueEntry[]>(QUEUE_KEY)) ?? [];
}

async function saveQueue(kv: KV, entries: QueueEntry[]): Promise<void> {
  await kv.set(QUEUE_KEY, entries);
}

/** Drop entries unseen for longer than QUEUE_TIMEOUT_MS; expire their sessions. */
async function pruneQueue(kv: KV, entries: QueueEntry[], now: number): Promise<QueueEntry[]> {
  const alive = entries.filter((e) => now - e.lastSeenAt <= QUEUE_TIMEOUT_MS);
  for (const dead of entries) {
    if (!alive.includes(dead)) {
      const sess = await kv.get<SessionRec>(sessKey(dead.playerId));
      if (sess && sess.queued && !sess.roomId) {
        await kv.set(sessKey(dead.playerId), { ...sess, queued: false });
      }
    }
  }
  return alive;
}

function makeRoom(a: EnginePlayer, b: EnginePlayer | null, code: string | null, now: number): EngineRoom {
  return {
    id: crypto.randomUUID(),
    code,
    players: b ? [a, b] : [a],
    phase: 'waiting',
    roundIndex: 0,
    level: 1,
    round: null,
    roundEnd: null,
    roundEndedAt: null,
    matchEnd: null,
    createdAt: now,
    updatedAt: now,
  };
}

async function saveRoom(kv: KV, room: EngineRoom): Promise<void> {
  await kv.set(roomKey(room.id), room);
}

function toRoundPayload(round: EngineRound): RoundStartPayload {
  return {
    index: round.index,
    digits: round.digits,
    target: round.target,
    timeLimitMs: round.timeLimitMs,
    endsAt: round.endsAt,
    difficultyCoefficient: round.difficultyCoefficient,
  };
}

function scoresOf(room: EngineRoom): Record<string, number> {
  const s: Record<string, number> = {};
  for (const p of room.players) s[p.id] = p.score;
  return s;
}

function winsOf(room: EngineRoom): Record<string, number> {
  const w: Record<string, number> = {};
  for (const p of room.players) w[p.id] = p.roundWins;
  return w;
}

function matchPayload(room: EngineRoom, meId: string): MatchStartPayload {
  const other = room.players.find((p) => p.id !== meId) ?? room.players[0];
  if (!other) throw new Error('room has no players');
  return {
    roomCode: room.code,
    opponent: { id: other.id, name: other.name },
    youAre: meId,
    bestOf: BEST_OF,
    roundsToWin: ROUNDS_TO_WIN,
  };
}

function beginRound(room: EngineRoom, now: number): void {
  room.phase = 'round-active';
  room.roundIndex += 1;
  room.level = Math.max(1, Math.min(6, room.roundIndex));
  const gen = generateRound(room.level);
  const cfg = configForLevel(room.level);
  const timeLimitMs = cfg.timeLimitSec * 1000;
  room.round = {
    index: room.roundIndex,
    digits: gen.digits,
    target: gen.target,
    solution: gen.solution,
    timeLimitMs,
    startedAt: now,
    endsAt: now + timeLimitMs,
    difficultyCoefficient: difficultyCoefficient(gen.digits.length, cfg.targetMax),
  };
  room.roundEnd = null;
  room.roundEndedAt = null;
  room.updatedAt = now;
}

function matchOver(room: EngineRoom): boolean {
  return (
    (room.players[0]?.roundWins ?? 0) >= ROUNDS_TO_WIN ||
    (room.players[1]?.roundWins ?? 0) >= ROUNDS_TO_WIN ||
    room.roundIndex >= BEST_OF
  );
}

function finish(room: EngineRoom, reason: 'rounds' | 'forfeit', winnerId: string | null, now: number): void {
  room.phase = 'ended';
  room.matchEnd = { winnerId, reason, scoresByPlayer: scoresOf(room), winsByPlayer: winsOf(room) };
  room.updatedAt = now;
}

function resolveTimeout(room: EngineRoom, now: number): void {
  room.phase = 'between-rounds';
  room.roundEndedAt = now;
  room.roundEnd = {
    index: room.roundIndex,
    winnerId: null,
    winningExpression: null,
    sampleSolution: room.round?.solution ?? null,
    scoresByPlayer: scoresOf(room),
    winsByPlayer: winsOf(room),
  };
  room.updatedAt = now;
}

function declareWinner(room: EngineRoom, winnerIdx: number, expression: string, receivedAt: number): void {
  const round = room.round;
  if (!round) return;
  const remainingMs = Math.max(0, round.endsAt - receivedAt);
  const points = roundScore(
    round.digits.length,
    configForLevel(room.level).targetMax,
    remainingMs,
    round.timeLimitMs,
  );
  room.players[winnerIdx]!.score += points;
  room.players[winnerIdx]!.roundWins += 1;
  room.phase = 'between-rounds';
  room.roundEndedAt = receivedAt;
  room.roundEnd = {
    index: room.roundIndex,
    winnerId: room.players[winnerIdx]!.id,
    winningExpression: expression,
    sampleSolution: round.solution,
    scoresByPlayer: scoresOf(room),
    winsByPlayer: winsOf(room),
  };
  room.updatedAt = receivedAt;
}

/**
 * Lazily advance a room to `now`: resolve due timeouts and start due rounds.
 * Returns 'expired' when a lone waiting host ran out of TTL (room deleted).
 */
async function touchRoom(
  kv: KV,
  room: EngineRoom,
  meId: string,
  now: number,
): Promise<{ room: EngineRoom } | { expired: true }> {
  const me = room.players.find((p) => p.id === meId);
  if (me) me.lastSeenAt = now;

  if (room.phase === 'waiting') {
    if (now - room.createdAt > WAITING_TTL_MS) {
      if (room.code) await kv.del(codeKey(room.code));
      await kv.del(roomKey(room.id));
      const sess = await kv.get<SessionRec>(sessKey(meId));
      if (sess) await kv.set(sessKey(meId), { ...sess, roomId: null });
      return { expired: true };
    }
    room.updatedAt = now;
    await saveRoom(kv, room);
    return { room };
  }

  if (room.phase === 'ended') {
    await saveRoom(kv, room);
    return { room };
  }

  const other = room.players.find((p) => p.id !== meId);
  // Stale opponent forfeits — replaces socket disconnect handling.
  if (other && now - other.lastSeenAt > OPPONENT_TIMEOUT_MS) {
    finish(room, 'forfeit', meId, now);
    await saveRoom(kv, room);
    return { room };
  }

  // Resolve every due transition (a long poll gap may cover several).
  for (let i = 0; i < BEST_OF + 2; i++) {
    if (room.phase === 'round-active' && room.round && now >= room.round.endsAt) {
      resolveTimeout(room, room.round.endsAt);
    }
    if (room.phase === 'between-rounds' && room.roundEndedAt !== null && now - room.roundEndedAt >= BETWEEN_ROUNDS_MS) {
      if (matchOver(room)) {
        const aWins = room.players[0]?.roundWins ?? 0;
        const bWins = room.players[1]?.roundWins ?? 0;
        const winner = aWins === bWins ? null : aWins > bWins ? (room.players[0] ?? null) : (room.players[1] ?? null);
        finish(room, 'rounds', winner?.id ?? null, now);
        break;
      }
      beginRound(room, now);
      break;
    }
    break;
  }

  await saveRoom(kv, room);
  return { room };
}

function playingSnapshot(room: EngineRoom, meId: string): Snapshot {
  return { kind: 'playing', match: matchPayload(room, meId), round: toRoundPayload(room.round!), roundEnd: room.roundEnd };
}

/** Try to pair `entry` with the oldest waiting entry. Returns the room on success. */
async function tryPair(kv: KV, entries: QueueEntry[], meId: string, now: number): Promise<EngineRoom | null> {
  const me = entries.find((e) => e.playerId === meId);
  const other = entries.find((e) => e.playerId !== meId);
  if (!me || !other) return null;
  const rest = entries.filter((e) => e.playerId !== meId && e.playerId !== other.playerId);
  const mk = (e: QueueEntry): EnginePlayer => ({
    id: e.playerId, name: e.name, token: e.token, score: 0, roundWins: 0, lastSeenAt: now,
  });
  const room = makeRoom(mk(other), mk(me), null, now);
  beginRound(room, now);
  await saveRoom(kv, room);
  await saveQueue(kv, rest);
  await kv.set(sessKey(me.playerId), { token: me.token, roomId: room.id, queued: false });
  await kv.set(sessKey(other.playerId), { token: other.token, roomId: room.id, queued: false });
  return room;
}

export interface Authed {
  playerId: string;
  sess: SessionRec;
}

async function auth(kv: KV, playerId: string, token: string): Promise<Authed | null> {
  const sess = await kv.get<SessionRec>(sessKey(playerId));
  if (!sess || sess.token !== token) return null;
  return { playerId, sess };
}

export async function quickplay(
  kv: KV,
  rawName: unknown,
  now: number = Date.now(),
): Promise<{ playerId: string; token: string; snapshot: Snapshot }> {
  const name = sanitizeName(rawName) ?? randomName();
  const { playerId, token } = newIds();
  let entries = await pruneQueue(kv, await loadQueue(kv), now);
  entries.push({ playerId, name, token, enqueuedAt: now, lastSeenAt: now });
  const room = await tryPair(kv, entries, playerId, now);
  if (room) {
    return { playerId, token, snapshot: playingSnapshot(room, playerId) };
  }
  await saveQueue(kv, entries);
  await kv.set(sessKey(playerId), { token, roomId: null, queued: true });
  const position = entries.findIndex((e) => e.playerId === playerId) + 1;
  return { playerId, token, snapshot: { kind: 'queued', position } };
}

export async function createPrivate(
  kv: KV,
  rawName: unknown,
  now: number = Date.now(),
): Promise<{ playerId: string; token: string; code: string; snapshot: Snapshot }> {
  const name = sanitizeName(rawName) ?? randomName();
  const { playerId, token } = newIds();
  let code = newCode();
  for (let i = 0; i < 5 && (await kv.get<string>(codeKey(code))); i++) code = newCode();
  const host: EnginePlayer = { id: playerId, name, token, score: 0, roundWins: 0, lastSeenAt: now };
  const room = makeRoom(host, null, code, now);
  await saveRoom(kv, room);
  await kv.set(codeKey(code), room.id);
  await kv.set(sessKey(playerId), { token, roomId: room.id, queued: false });
  return { playerId, token, code, snapshot: { kind: 'waiting', code } };
}

export async function joinPrivate(
  kv: KV,
  rawName: unknown,
  rawCode: unknown,
  now: number = Date.now(),
): Promise<{ playerId: string; token: string; snapshot: Snapshot } | { error: JoinError }> {
  const code = typeof rawCode === 'string' ? rawCode.trim().toUpperCase() : '';
  const roomId = code ? await kv.get<string>(codeKey(code)) : null;
  if (!roomId) return { error: 'not-found' };
  const room = await kv.get<EngineRoom>(roomKey(roomId));
  if (!room || room.phase !== 'waiting' || room.players.length !== 1) {
    await kv.del(codeKey(code));
    return { error: 'full' };
  }
  if (now - room.createdAt > WAITING_TTL_MS) {
    await kv.del(codeKey(code));
    await kv.del(roomKey(room.id));
    return { error: 'expired' };
  }
  const name = sanitizeName(rawName) ?? randomName();
  const { playerId, token } = newIds();
  room.players.push({ id: playerId, name, token, score: 0, roundWins: 0, lastSeenAt: now });
  beginRound(room, now);
  await saveRoom(kv, room);
  await kv.del(codeKey(code));
  await kv.set(sessKey(playerId), { token, roomId: room.id, queued: false });
  return { playerId, token, snapshot: playingSnapshot(room, playerId) };
}

export async function getSnapshot(
  kv: KV,
  playerId: string,
  token: string,
  now: number = Date.now(),
): Promise<Snapshot> {
  const a = await auth(kv, playerId, token);
  if (!a) return { kind: 'idle' };

  if (a.sess.queued && !a.sess.roomId) {
  let entries = await pruneQueue(kv, await loadQueue(kv), now);
    const me = entries.find((e) => e.playerId === playerId);
    if (!me) return { kind: 'idle', message: 'Queue expired, try again.' };
    me.lastSeenAt = now;
    const room = await tryPair(kv, entries, playerId, now);
    if (room) return playingSnapshot(room, playerId);
    await saveQueue(kv, entries);
    return { kind: 'queued', position: entries.findIndex((e) => e.playerId === playerId) + 1 };
  }

  if (!a.sess.roomId) return { kind: 'idle' };
  const room = await kv.get<EngineRoom>(roomKey(a.sess.roomId));
  if (!room) return { kind: 'idle' };
  const touched = await touchRoom(kv, room, playerId, now);
  if ('expired' in touched) return { kind: 'idle', message: 'Room expired.' };
  const r = touched.room;
  if (r.phase === 'waiting') return { kind: 'waiting', code: r.code ?? '' };
  if (r.phase === 'ended') return { kind: 'ended', match: r.players.length === 2 ? matchPayload(r, playerId) : null, result: r.matchEnd! };
  return playingSnapshot(r, playerId);
}

export async function submit(
  kv: KV,
  playerId: string,
  token: string,
  expression: unknown,
  now: number = Date.now(),
): Promise<{ accepted: boolean; message?: string; snapshot: Snapshot }> {
  const a = await auth(kv, playerId, token);
  if (!a || !a.sess.roomId) return { accepted: false, message: 'Not in an active match.', snapshot: { kind: 'idle' } };
  const room = await kv.get<EngineRoom>(roomKey(a.sess.roomId));
  if (!room) return { accepted: false, message: 'Not in an active match.', snapshot: { kind: 'idle' } };
  const touched = await touchRoom(kv, room, playerId, now);
  if ('expired' in touched) return { accepted: false, message: 'Room expired.', snapshot: { kind: 'idle', message: 'Room expired.' } };
  const r = touched.room;
  if (r.phase === 'ended') {
    return {
      accepted: false,
      snapshot: { kind: 'ended', match: matchPayload(r, playerId), result: r.matchEnd! },
    };
  }
  if (r.phase !== 'round-active' || !r.round) {
    return { accepted: false, snapshot: playingSnapshot(r, playerId) };
  }
  const side = r.players.findIndex((p) => p.id === playerId);
  if (side === -1) return { accepted: false, message: 'Not in an active match.', snapshot: { kind: 'idle' } };

  // Server is source of truth — re-validate regardless of client claims.
  const result = validateExpression(String(expression ?? ''), r.round.digits, r.round.target);
  if (!result.valid) {
    return { accepted: false, message: `Invalid expression: ${result.reason}`, snapshot: playingSnapshot(r, playerId) };
  }
  declareWinner(r, side, String(expression), now);
  if (matchOver(r)) {
    const aWins = r.players[0]?.roundWins ?? 0;
    const bWins = r.players[1]?.roundWins ?? 0;
    const winner = aWins === bWins ? null : aWins > bWins ? (r.players[0] ?? null) : (r.players[1] ?? null);
    finish(r, 'rounds', winner?.id ?? null, now);
    await saveRoom(kv, r);
    return {
      accepted: true,
      snapshot: { kind: 'ended', match: matchPayload(r, playerId), result: r.matchEnd! },
    };
  }
  await saveRoom(kv, r);
  return { accepted: true, snapshot: playingSnapshot(r, playerId) };
}

export async function leave(
  kv: KV,
  playerId: string,
  token: string,
  now: number = Date.now(),
): Promise<{ ok: boolean }> {
  const a = await auth(kv, playerId, token);
  if (!a) return { ok: true };
  if (a.sess.queued && !a.sess.roomId) {
    const entries = (await loadQueue(kv)).filter((e) => e.playerId !== playerId);
    await saveQueue(kv, entries);
  }
  if (a.sess.roomId) {
    const room = await kv.get<EngineRoom>(roomKey(a.sess.roomId));
    if (room && room.phase !== 'ended') {
      const other = room.players.find((p) => p.id !== playerId);
      finish(room, 'forfeit', other?.id ?? null, now);
      await saveRoom(kv, room);
    }
  }
  await kv.del(sessKey(playerId));
  return { ok: true };
}
