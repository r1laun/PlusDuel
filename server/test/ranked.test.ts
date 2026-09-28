import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Hub } from '../src/hub.js';
import type { Player } from '../src/match.js';
import { findSolution, type MatchEndPayload, type RoundStartPayload } from '@plusduel/shared';

function fakeSocket(id: string) {
  const emitted: { event: string; payload: any }[] = [];
  return {
    id,
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
  it('awards Elo on match end and serves rankings', () => {
    const hub = new Hub();
    const sa = fakeSocket('sa');
    const sb = fakeSocket('sb');
    hub.enqueue(player(sa, 'Alice', 'pa'));
    hub.enqueue(player(sb, 'Bob', 'pb'));

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

    const rankings = hub.rankingsFor('pa');
    expect(rankings.top).toHaveLength(2);
    expect(rankings.top[0]!.playerId).toBe('pa');
    expect(rankings.you!.rating).toBe(ratings['sa']!.after);
  });

  it('private rooms stay unrated', () => {
    const hub = new Hub();
    const host = fakeSocket('h');
    const guest = fakeSocket('g');
    const code = hub.createPrivate(player(host, 'Host', 'ph'))!;
    expect(hub.joinPrivate(player(guest, 'Guest', 'pg'), code)).toBe(true);

    hub.handleDisconnect('h');
    const ends = endsOf(guest.emitted);
    expect(ends).toHaveLength(1);
    expect(ends[0]!.ratingsByPlayer).toBeUndefined();
    expect(hub.rankingsFor('pg').top).toHaveLength(0);
  });
});
