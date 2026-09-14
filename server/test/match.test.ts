import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Room, type Player } from '../src/match.js';
import { findSolution, type RoundStartPayload, type MatchEndPayload } from '@plusduel/shared';

function fakePlayer(id: string, name: string) {
  const emitted: { event: string; payload: any }[] = [];
  const socket = {
    id,
    emit: (event: string, payload: any) => {
      emitted.push({ event, payload });
    },
  };
  const player = { socket, name, score: 0, roundWins: 0 } as unknown as Player;
  return { player, emitted };
}

const startsOf = (emitted: { event: string; payload: any }[]) =>
  emitted.filter((e) => e.event === 'round:start').map((e) => e.payload as RoundStartPayload);
const endsOf = (emitted: { event: string; payload: any }[]) =>
  emitted.filter((e) => e.event === 'match:end').map((e) => e.payload as MatchEndPayload);

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe('Room match length', () => {
  it('ends in a draw after 5 timeouts — no round 6', () => {
    const a = fakePlayer('a', 'A');
    const b = fakePlayer('b', 'B');
    new Room(a.player, b.player).start();

    // Let 5 rounds time out (level time limits grow: 60s → 75s → 90s…).
    for (let r = 1; r <= 5; r++) {
      for (let i = 0; i < 30; i++) {
        vi.advanceTimersByTime(10_000);
        if (endsOf(a.emitted).length > 0 || startsOf(a.emitted).length > r) break;
      }
    }

    expect(startsOf(a.emitted)).toHaveLength(5);
    const ends = endsOf(a.emitted);
    expect(ends).toHaveLength(1);
    expect(ends[0]!.reason).toBe('rounds');
    expect(ends[0]!.winnerId).toBeNull();

    // Regression: previously rounds continued past 5 forever.
    vi.advanceTimersByTime(600_000);
    expect(startsOf(a.emitted)).toHaveLength(5);
    expect(endsOf(a.emitted)).toHaveLength(1);
    expect(endsOf(b.emitted)).toHaveLength(1);
    expect(endsOf(b.emitted)[0]!.winnerId).toBeNull();
  });

  it('ends with a winner at 3 round wins', () => {
    const a = fakePlayer('a', 'A');
    const b = fakePlayer('b', 'B');
    const room = new Room(a.player, b.player);
    room.start();

    for (let w = 0; w < 3; w++) {
      const rs = startsOf(a.emitted).at(-1)!;
      const sol = findSolution(rs.digits, rs.target);
      expect(sol).not.toBeNull();
      room.submit('a', sol!);
      vi.advanceTimersByTime(3500);
    }

    const ends = endsOf(a.emitted);
    expect(ends).toHaveLength(1);
    expect(ends[0]!.reason).toBe('rounds');
    expect(ends[0]!.winnerId).toBe('a');
    expect(startsOf(a.emitted)).toHaveLength(3);
  });
});
