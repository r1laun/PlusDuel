import { describe, it, expect } from 'vitest';
import {
  BETWEEN_ROUNDS_MS,
  OPPONENT_TIMEOUT_MS,
  QUEUE_TIMEOUT_MS,
  createPrivate,
  getSnapshot,
  joinPrivate,
  leave,
  memoryKV,
  quickplay,
  roomKey,
  sessKey,
  submit,
  type EngineRoom,
  type KV,
} from '../src/engine.js';
import type { Snapshot } from '../src/api.js';

async function roomOf(kv: KV, playerId: string): Promise<EngineRoom> {
  const sess = await kv.get<{ roomId: string | null }>(sessKey(playerId));
  const room = await kv.get<EngineRoom>(roomKey(sess!.roomId!));
  return room!;
}

function playing(snap: Snapshot): Extract<Snapshot, { kind: 'playing' }> {
  if (snap.kind !== 'playing') throw new Error(`expected playing, got ${snap.kind}`);
  return snap;
}

interface TestPlayer {
  playerId: string;
  token: string;
}

/** Simulate the ~1s client poll loop: both players heartbeat in ≤10s steps. */
async function advance(kv: KV, a: TestPlayer, b: TestPlayer, fromT: number, toT: number): Promise<void> {
  let t = fromT;
  while (t < toT) {
    t = Math.min(t + 10_000, toT);
    await getSnapshot(kv, a.playerId, a.token, t);
    await getSnapshot(kv, b.playerId, b.token, t);
  }
}

describe('quickplay pairing', () => {
  it('pairs two queued players into the same round', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    expect(a.snapshot).toEqual({ kind: 'queued', position: 1 });
    const b = await quickplay(kv, 'Bob', t + 500);
    expect(b.snapshot.kind).toBe('playing');

    const sa = await getSnapshot(kv, a.playerId, a.token, t + 600);
    expect(sa.kind).toBe('playing');
    const pa = playing(sa);
    const pb = playing(b.snapshot);
    expect(pa.round.digits).toEqual(pb.round.digits);
    expect(pa.round.target).toBe(pb.round.target);
    expect(pa.match.youAre).toBe(a.playerId);
    expect(pb.match.youAre).toBe(b.playerId);
    expect(pa.match.opponent.name).toBe('Bob');
  });

  it('expires stale queue entries', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    // Alice never polls again; her entry goes stale.
    const b = await quickplay(kv, 'Bob', t + QUEUE_TIMEOUT_MS + 1000);
    expect(b.snapshot).toEqual({ kind: 'queued', position: 1 });
    const sa = await getSnapshot(kv, a.playerId, a.token, t + QUEUE_TIMEOUT_MS + 1100);
    expect(sa.kind).toBe('idle');
  });

  it('rejects bad tokens', async () => {
    const kv = memoryKV();
    const a = await quickplay(kv, 'Alice', 1_000_000);
    expect(await getSnapshot(kv, a.playerId, 'wrong-token', 1_000_001)).toEqual({ kind: 'idle' });
  });
});

describe('round flow', () => {
  it('first valid submission wins the round (sudden death)', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    const b = await quickplay(kv, 'Bob', t + 100);
    const room = await roomOf(kv, a.playerId);

    const res = await submit(kv, a.playerId, a.token, room.round!.solution, t + 2000);
    expect(res.accepted).toBe(true);
    const snap = playing(res.snapshot);
    expect(snap.roundEnd?.winnerId).toBe(a.playerId);
    expect(snap.roundEnd?.winningExpression).toBe(room.round!.solution);

    // Late submit after decision is rejected; the winner stands.
    const roomAfter = await roomOf(kv, b.playerId);
    const late = await submit(kv, b.playerId, b.token, roomAfter.round!.solution, t + 2100);
    expect(late.accepted).toBe(false);
    expect(playing(late.snapshot).roundEnd?.winnerId).toBe(a.playerId);
  });

  it('rejects invalid expressions without ending the round', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    await quickplay(kv, 'Bob', t + 100);
    const res = await submit(kv, a.playerId, a.token, '999+999', t + 500);
    expect(res.accepted).toBe(false);
    expect(res.message).toMatch(/Invalid expression/);
    expect(res.snapshot.kind).toBe('playing');
    expect(playing(res.snapshot).roundEnd).toBeNull();
  });

  it('times out idle rounds and advances to the next one', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    const b = await quickplay(kv, 'Bob', t + 100);
    const room = await roomOf(kv, a.playerId);
    const endsAt = room.round!.endsAt;

    // Simulate the live poll loop up to just before the deadline.
    await advance(kv, a, b, t + 600, endsAt - 500);
    const s1 = playing(await getSnapshot(kv, a.playerId, a.token, endsAt + 1));
    expect(s1.roundEnd?.winnerId).toBeNull();
    expect(s1.roundEnd?.sampleSolution).toBe(room.round!.solution);

    const s2 = playing(await getSnapshot(kv, a.playerId, a.token, endsAt + BETWEEN_ROUNDS_MS + 1));
    expect(s2.round.index).toBe(2);
    expect(s2.roundEnd).toBeNull();
  });

  it('ends the match at 3 round wins', async () => {
    const kv = memoryKV();
    let t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    const b = await quickplay(kv, 'Bob', t + 100);
    for (let i = 0; i < 3; i++) {
      const room = await roomOf(kv, a.playerId);
      t = room.round!.startedAt + 500;
      // Heartbeat so neither side looks abandoned across jumps.
      await getSnapshot(kv, a.playerId, a.token, t);
      await getSnapshot(kv, b.playerId, b.token, t);
      const res = await submit(kv, a.playerId, a.token, room.round!.solution, t);
      if (i < 2) {
        expect(res.snapshot.kind).toBe('playing');
        // Fast-forward past the between-rounds gap.
        t += BETWEEN_ROUNDS_MS + 10;
        await getSnapshot(kv, a.playerId, a.token, t);
      } else {
        expect(res.snapshot.kind).toBe('ended');
        if (res.snapshot.kind === 'ended') {
          expect(res.snapshot.result.winnerId).toBe(a.playerId);
          expect(res.snapshot.result.reason).toBe('rounds');
          expect(res.snapshot.result.winsByPlayer[a.playerId]).toBe(3);
        }
      }
    }
    const sb = await getSnapshot(kv, b.playerId, b.token, t + 10);
    expect(sb.kind).toBe('ended');
  });

  it('draws after 5 timeout rounds', async () => {
    const kv = memoryKV();
    let t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    const b = await quickplay(kv, 'Bob', t + 100);
    let snap: Snapshot = await getSnapshot(kv, a.playerId, a.token, t + 200);
    for (let i = 0; i < 5; i++) {
      const room = await roomOf(kv, a.playerId);
      // Live poll loop up to the deadline so the timeout (not forfeit) fires.
      await advance(kv, a, b, t, room.round!.endsAt - 500);
      t = room.round!.endsAt + BETWEEN_ROUNDS_MS + 10;
      snap = await getSnapshot(kv, a.playerId, a.token, t);
    }
    expect(snap.kind).toBe('ended');
    if (snap.kind === 'ended') {
      expect(snap.result.winnerId).toBeNull();
      expect(snap.result.reason).toBe('rounds');
    }
  });
});

describe('private rooms', () => {
  it('creates, joins by code, and starts the match', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const host = await createPrivate(kv, 'Host', t);
    expect(host.snapshot).toEqual({ kind: 'waiting', code: host.code });

    const bad = await joinPrivate(kv, 'Guest', 'ZZZZ', t + 100);
    expect(bad).toEqual({ error: 'not-found' });

    const guest = await joinPrivate(kv, 'Guest', host.code.toLowerCase(), t + 200);
    if (!('snapshot' in guest)) throw new Error('expected join success');
    expect(guest.snapshot.kind).toBe('playing');

    const sh = await getSnapshot(kv, host.playerId, host.token, t + 300);
    expect(sh.kind).toBe('playing');
    expect(playing(sh).match.roomCode).toBe(host.code);

    // Code is single-use.
    const again = await joinPrivate(kv, 'Third', host.code, t + 400);
    expect(again).toEqual({ error: 'not-found' });
  });
});

describe('forfeit', () => {
  it('explicit leave forfeits the match to the opponent', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    const b = await quickplay(kv, 'Bob', t + 100);
    await leave(kv, a.playerId, a.token, t + 5000);
    const sb = await getSnapshot(kv, b.playerId, b.token, t + 5100);
    expect(sb.kind).toBe('ended');
    if (sb.kind === 'ended') {
      expect(sb.result.winnerId).toBe(b.playerId);
      expect(sb.result.reason).toBe('forfeit');
    }
    expect(await getSnapshot(kv, a.playerId, a.token, t + 5200)).toEqual({ kind: 'idle' });
  });

  it('stale opponent forfeits (replaces socket disconnect)', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    const b = await quickplay(kv, 'Bob', t + 100);
    // Alice never polls again. Bob keeps polling; past the opponent timeout
    // Bob is awarded the match.
    const sb = await getSnapshot(kv, b.playerId, b.token, t + OPPONENT_TIMEOUT_MS + 5000);
    expect(sb.kind).toBe('ended');
    if (sb.kind === 'ended') {
      expect(sb.result.winnerId).toBe(b.playerId);
      expect(sb.result.reason).toBe('forfeit');
    }
    void a;
  });

  it('leave while queued removes the queue entry', async () => {
    const kv = memoryKV();
    const t = 1_000_000;
    const a = await quickplay(kv, 'Alice', t);
    await leave(kv, a.playerId, a.token, t + 100);
    const b = await quickplay(kv, 'Bob', t + 200);
    expect(b.snapshot).toEqual({ kind: 'queued', position: 1 });
  });
});
