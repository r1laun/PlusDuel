import {
  START_RATING,
  eloUpdate,
  kFor,
  titleFor,
  type LeaderboardEntry,
  type MatchScore,
} from '@plusduel/shared';

export interface RatingRecord {
  playerId: string;
  name: string;
  rating: number;
  games: number;
  wins: number;
  losses: number;
  draws: number;
}

/**
 * In-memory ranked ladder (Quick play only).
 * NOTE: wiped on server restart — acceptable for now, persistent DB is phase 2.
 */
export class RatingsStore {
  private records = new Map<string, RatingRecord>();

  /** Get existing record or a fresh provisional one. Never throws. */
  for(playerId: string, name: string): RatingRecord {
    const id = playerId.trim().slice(0, 64) || 'anonymous';
    let rec = this.records.get(id);
    if (!rec) {
      rec = { playerId: id, name, rating: START_RATING, games: 0, wins: 0, losses: 0, draws: 0 };
      this.records.set(id, rec);
    }
    rec.name = name;
    return rec;
  }

  peek(playerId: string): RatingRecord | undefined {
    return this.records.get(playerId.trim().slice(0, 64));
  }

  /**
   * Apply a rated result. scoreA is from A's perspective (1 / 0.5 / 0).
   * Returns per-player {before, after}.
   */
  recordMatch(a: RatingRecord, b: RatingRecord, scoreA: MatchScore): { a: { before: number; after: number }; b: { before: number; after: number } } {
    return applyRatedResult(a, b, scoreA);
  }

  top(limit: number): LeaderboardEntry[] {
    return [...this.records.values()]
      .filter((r) => r.games > 0)
      .sort((x, y) => y.rating - x.rating || x.games - y.games)
      .slice(0, limit)
      .map(toEntry);
  }

  entry(rec: RatingRecord): LeaderboardEntry {
    return toEntry(rec);
  }
}
function toEntry(r: RatingRecord): LeaderboardEntry {
  return {
    playerId: r.playerId,
    name: r.name,
    rating: r.rating,
    title: titleFor(r.rating),
    games: r.games,
    wins: r.wins,
    losses: r.losses,
    draws: r.draws,
  };
}

/** Standalone result application (used by Room without touching the store). */
export function applyRatedResult(
  a: RatingRecord,
  b: RatingRecord,
  scoreA: MatchScore,
): { a: { before: number; after: number }; b: { before: number; after: number } } {
  const beforeA = a.rating;
  const beforeB = b.rating;
  const afterA = eloUpdate(beforeA, beforeB, scoreA, kFor(a.games));
  const afterB = eloUpdate(beforeB, beforeA, ((1 - scoreA) as MatchScore), kFor(b.games));
  a.rating = afterA;
  b.rating = afterB;
  a.games += 1;
  b.games += 1;
  if (scoreA === 1) {
    a.wins += 1;
    b.losses += 1;
  } else if (scoreA === 0) {
    a.losses += 1;
    b.wins += 1;
  } else {
    a.draws += 1;
    b.draws += 1;
  }
  return { a: { before: beforeA, after: afterA }, b: { before: beforeB, after: afterB } };
}
