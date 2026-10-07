import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Hub } from '../src/hub.js';
import type { Player } from '../src/match.js';
import type { RatingRecord } from '../src/ratings.js';
import type { RatingsDb } from '../src/supabase.js';
import { findSolution, type LeaderboardEntry, type MatchEndPayload, type RoundStartPayload } from '@plusduel/shared';

/** In-memory RatingsDb stand-in (no network). */
class FakeDb implements RatingsDb {
  saved = new Map<string, RatingRecord>();
  async load(playerId: string): Promise<RatingRecord | null> {
    const rec = this.saved.get(playerId);
    return rec ? { ...rec } : null;
  }
  async save(rec: RatingRecord): Promise<void> {
    this.saved.set(rec.playerId, { ...rec });
  }
  async remove(playerId: string): Promise<void> {
    this.saved.delete(playerId);
  }
  async top(limit: number): Promise<LeaderboardEntry[]> {
    return [...this.saved.values()]
      .filter((r) => r.games > 0)
      .sort((x, y) => y.rating - x.rating)
      .slice(0, limit)
      .map((r) => ({
        playerId: r.playerId,
        name: r.name,
        rating: r.rating,
        title: 'Gold',
        games: r.games,
        wins: r.wins,
        losses: r.losses,
        draws: r.draws,
      }));
  }
}

function fakeSocket(id: string) {
  const emitted: { event: string; payload: any }[] = [];
  return {
    id,
    connected: true,
    emit: (event: string, payload: any) => {
      emitted.push({ event, payload });
    },
    emitted,
  };
}

function player(sock: ReturnType<typeof fakeSocket>, name: string, playerId: string): Player {
  return { socket: sock as any, name, playerId, score: 0, roundWins: 0 };
}

const endsOf = (emitted: { event: string; payload: any }[]) =>
  emitted.filter((e) => e.event === 'match:end').map((e) => e.payload as MatchEndPayload);

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe('ranked quick play', () => {
  it('awards Elo on match end and serves rankings', async () => {
    const hub = new Hub();
    const sa = fakeSocket('sa');
    const sb = fakeSocket('sb');
    hub.enqueue(player(sa, 'Alice', 'pa'));
    hub.enqueue(player(sb, 'Bob', 'pb'));
    // Record loading is async - flush microtasks (fake timers stay on for rounds).
    for (let i = 0; i < 10; i++) await Promise.resolve();

    const room = hub.roomForSocket('sa')!;
    expect(room).toBeDefined();

    for (let w = 0; w < 3; w++) {
      const rs = sa.emitted.filter((e) => e.event === 'round:start').at(-1)!.payload as RoundStartPayload;
      room.submit('sa', findSolution(rs.digits, rs.target)!);
      vi.advanceTimersByTime(3500);
    }

    const ends = endsOf(sa.emitted);
    expect(ends).toHaveLength(1);
    const ratings = ends[0]!.ratingsByPlayer!;
    expect(ratings).toBeDefined();
    expect(ratings['sa']!.after).toBeGreaterThan(ratings['sa']!.before);
    expect(ratings['sb']!.after).toBeLessThan(ratings['sb']!.before);

    const rankings = await hub.rankingsFor('pa');
    expect(rankings.top).toHaveLength(2);
    expect(rankings.top[0]!.playerId).toBe('pa');
    expect(rankings.you!.rating).toBe(ratings['sa']!.after);
  });

  it('private rooms stay unrated', async () => {
    const hub = new Hub();
    const host = fakeSocket('h');
    const guest = fakeSocket('g');
    const code = hub.createPrivate(player(host, 'Host', 'ph'))!;
    expect(hub.joinPrivate(player(guest, 'Guest', 'pg'), code)).toBe(true);

    hub.handleDisconnect('h');
    const ends = endsOf(guest.emitted);
    expect(ends).toHaveLength(1);
    expect(ends[0]!.ratingsByPlayer).toBeUndefined();
    expect((await hub.rankingsFor('pg')).top).toHaveLength(0);
  });

  it('links a guest record into the account on first sign-in', async () => {
    const db = new FakeDb();
    const hub = new Hub(db);
    // Guest played two rated games (stored in memory under the device id).
    hub.ratings.recordMatch(hub.ratings.for('dev-1', 'Guest'), hub.ratings.for('dev-2', 'Opp'), 1);
    hub.ratings.recordMatch(hub.ratings.for('dev-1', 'Guest'), hub.ratings.for('dev-3', 'Opp2'), 1);

    const entry = await hub.linkAccount('user-1', 'dev-1', 'Alice');
    expect(entry).not.toBeNull();
    expect(entry!.rating).toBeGreaterThan(1000);
    expect(entry!.games).toBe(2);
    // Guest row is gone - no double counting in the ladder.
    expect(hub.ratings.peek('dev-1')).toBeUndefined();
    expect(db.saved.get('acct:user-1')!.rating).toBe(entry!.rating);

    // Second sign-in with no new guest games keeps the account as-is.
    const again = await hub.linkAccount('user-1', 'dev-1', 'Alice');
    expect(again!.rating).toBe(entry!.rating);
    expect(again!.games).toBe(2);
  });

  it('merges guest stats into an existing account keeping the best rating', async () => {
    const db = new FakeDb();
    db.saved.set('acct:user-9', {
      playerId: 'acct:user-9',
      name: 'Pro',
      rating: 1500,
      games: 20,
      wins: 15,
      losses: 5,
      draws: 0,
    });
    const hub = new Hub(db);
    hub.ratings.recordMatch(hub.ratings.for('dev-9', 'ProGuest'), hub.ratings.for('dev-x', 'Opp'), 0);

    const entry = await hub.linkAccount('user-9', 'dev-9', 'Pro');
    expect(entry!.rating).toBe(1500);
    expect(entry!.games).toBe(21);
    expect(entry!.losses).toBe(6);
  });

  it('persists guest records to the DB so the ladder survives restarts', async () => {
    const db = new FakeDb();
    const hub = new Hub(db);
    const sa = fakeSocket('sa');
    const sb = fakeSocket('sb');
    hub.enqueue(player(sa, 'Alice', 'dev-a'));
    hub.enqueue(player(sb, 'Bob', 'dev-b'));
    for (let i = 0; i < 10; i++) await Promise.resolve();

    const room = hub.roomForSocket('sa')!;
    expect(room).toBeDefined();
    for (let w = 0; w < 3; w++) {
      const rs = sa.emitted.filter((e) => e.event === 'round:start').at(-1)!.payload as RoundStartPayload;
      room.submit('sa', findSolution(rs.digits, rs.target)!);
      vi.advanceTimersByTime(3500);
    }
    expect(endsOf(sa.emitted)).toHaveLength(1);
    // onSettled persists async - flush before asserting.
    for (let i = 0; i < 10; i++) await Promise.resolve();

    expect(db.saved.get('dev-a')!.games).toBeGreaterThan(0);
    expect(db.saved.get('dev-b')!.games).toBeGreaterThan(0);
    // A fresh hub over the same DB still sees the ladder (restart survival).
    const hub2 = new Hub(db);
    const rankings = await hub2.rankingsFor('dev-a');
    expect(rankings.top).toHaveLength(2);
    expect(rankings.you!.rating).toBe(db.saved.get('dev-a')!.rating);
  });

  it('rejects the same player queuing on a second tab', async () => {
    const hub = new Hub();
    const s1 = fakeSocket('s1');
    const s2 = fakeSocket('s2');
    hub.enqueue(player(s1, 'Me', 'same-id'));
    hub.enqueue(player(s2, 'MeAgain', 'same-id'));

    const errors = s2.emitted.filter((e) => e.event === 'game:error');
    expect(errors).toHaveLength(1);
    expect(errors[0]!.payload.message).toMatch(/another tab/);
    // No room was created for the duplicate.
    for (let i = 0; i < 10; i++) await Promise.resolve();
    expect(hub.roomForSocket('s1')).toBeUndefined();
    expect(hub.roomForSocket('s2')).toBeUndefined();
  });

  it('rates matches between different players (even from one machine)', async () => {
    const hub = new Hub();
    const sa = fakeSocket('sa');
    const sb = fakeSocket('sb');
    hub.enqueue(player(sa, 'Alice', 'dev-a'));
    hub.enqueue(player(sb, 'Bob', 'dev-b'));
    for (let i = 0; i < 10; i++) await Promise.resolve();

    const room = hub.roomForSocket('sa')!;
    expect(room).toBeDefined();
    const starts = sa.emitted.filter((e) => e.event === 'match:start');
    expect(starts).toHaveLength(1);
    expect(starts[0]!.payload.rated).toBe(true);

    for (let w = 0; w < 3; w++) {
      const rs = sa.emitted.filter((e) => e.event === 'round:start').at(-1)!.payload as RoundStartPayload;
      room.submit('sa', findSolution(rs.digits, rs.target)!);
      vi.advanceTimersByTime(3500);
    }
    const ends = endsOf(sa.emitted);
    expect(ends).toHaveLength(1);
    expect(ends[0]!.ratingsByPlayer!['sa']!.after).toBeGreaterThan(1000);
  });
});
