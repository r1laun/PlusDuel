import { describe, expect, it } from 'vitest';
import { eloUpdate, kFor, titleFor } from '@plusduel/shared';
import { RatingsStore } from '../src/ratings.js';

describe('eloUpdate', () => {
  it('favourite gains little, underdog gains a lot', () => {
    const favWin = eloUpdate(1400, 1000, 1, 32);
    const dogWin = eloUpdate(1000, 1400, 1, 32);
    expect(favWin - 1400).toBeLessThan(dogWin - 1000);
    expect(favWin).toBeGreaterThan(1400);
  });

  it('equal players split K/2 on win', () => {
    expect(eloUpdate(1000, 1000, 1, 32)).toBe(1016);
    expect(eloUpdate(1000, 1000, 0, 32)).toBe(984);
    expect(eloUpdate(1000, 1000, 0.5, 32)).toBe(1000);
  });

  it('zero-sum: winner gain equals loser loss', () => {
    const a = eloUpdate(1200, 1100, 1, 32);
    const b = eloUpdate(1100, 1200, 0, 32);
    expect(a - 1200).toBe(1100 - b);
  });
});

describe('kFor', () => {
  it('provisional K for the first 10 games', () => {
    expect(kFor(0)).toBe(40);
    expect(kFor(9)).toBe(40);
    expect(kFor(10)).toBe(32);
  });
});

describe('titleFor', () => {
  it('maps rating bands to titles', () => {
    expect(titleFor(500)).toBe('Novice');
    expect(titleFor(800)).toBe('Club');
    expect(titleFor(1000)).toBe('Gold');
    expect(titleFor(1200)).toBe('Platinum');
    expect(titleFor(1400)).toBe('Diamond');
    expect(titleFor(1600)).toBe('Master');
    expect(titleFor(1800)).toBe('Grandmaster');
    expect(titleFor(2500)).toBe('Grandmaster');
  });
});

describe('RatingsStore', () => {
  it('records wins/losses and serves a sorted top list', () => {
    const store = new RatingsStore();
    const a = store.for('a', 'Alice');
    const b = store.for('b', 'Bob');
    const res = store.recordMatch(a, b, 1);
    expect(res.a.after).toBeGreaterThan(res.a.before);
    expect(res.b.after).toBeLessThan(res.b.before);
    expect(a.wins).toBe(1);
    expect(b.losses).toBe(1);

    const top = store.top(10);
    expect(top).toHaveLength(2);
    expect(top[0]!.playerId).toBe('a');
    expect(top[0]!.title).toBe(titleFor(top[0]!.rating));
  });

  it('draw moves both ratings toward each other', () => {
    const store = new RatingsStore();
    const a = store.for('a', 'Alice');
    const b = store.for('b', 'Bob');
    b.rating = 1400;
    const res = store.recordMatch(a, b, 0.5);
    expect(res.a.after).toBeGreaterThan(res.a.before);
    expect(res.b.after).toBeLessThan(res.b.before);
    expect(a.draws).toBe(1);
  });

  it('unplayed records stay out of the leaderboard', () => {
    const store = new RatingsStore();
    store.for('ghost', 'Ghost');
    expect(store.top(10)).toHaveLength(0);
    expect(store.peek('ghost')!.games).toBe(0);
  });
});
