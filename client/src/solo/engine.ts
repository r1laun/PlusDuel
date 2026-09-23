import type {
  MatchEndPayload,
  MatchStartPayload,
  RoundEndPayload,
  RoundStartPayload,
} from '@plusduel/shared';

export type SoloMode = 'endless' | 'match';

/** Synthetic ids — no socket involved in solo. */
export const SOLO_YOU = 'solo-you';
export const SOLO_CLOCK = 'solo-clock';

/**
 * Match rules mirror server/src/match.ts (BEST_OF / ROUNDS_TO_WIN /
 * BETWEEN_ROUNDS_MS). Kept in sync manually — solo never talks to the server.
 */
export const SOLO_BEST_OF = 5;
export const SOLO_ROUNDS_TO_WIN = 3;
export const SOLO_BETWEEN_ROUNDS_MS = 3500;

export interface SoloRound {
  index: number;
  digits: number[];
  target: number;
  solution: string;
  timeLimitMs: number;
  difficultyCoefficient: number;
  startedAt: number;
}

/**
 * Generate one solo round. The generator (fraction.js) is lazy-imported so it
 * stays out of the initial bundle — same approach as the lazy validator.
 */
export async function createSoloRound(index: number, level: number): Promise<SoloRound> {
  const { generateRound, configForLevel, difficultyCoefficient } = await import('@plusduel/shared');
  const cfg = configForLevel(level);
  const gen = generateRound(level);
  return {
    index,
    digits: gen.digits,
    target: gen.target,
    solution: gen.solution,
    timeLimitMs: cfg.timeLimitSec * 1000,
    difficultyCoefficient: difficultyCoefficient(gen.digits.length, cfg.targetMax),
    startedAt: Date.now(),
  };
}

/** Level used for a match-mode round — climbs with the round like in duels. */
export function levelForMatchRound(roundIndex: number): number {
  return Math.max(1, Math.min(6, roundIndex));
}

export async function validateSoloExpression(
  expression: string,
  digits: number[],
  target: number,
): Promise<{ valid: boolean; reason?: string }> {
  const { validateExpression } = await import('@plusduel/shared/validator');
  const r = validateExpression(expression, digits, target);
  return { valid: r.valid, reason: r.reason };
}

export async function scoreSoloRound(
  level: number,
  digitCount: number,
  remainingMs: number,
  timeLimitMs: number,
): Promise<number> {
  const { roundScore, configForLevel } = await import('@plusduel/shared');
  return roundScore(digitCount, configForLevel(level).targetMax, remainingMs, timeLimitMs);
}

export function soloMatchInfo(): MatchStartPayload {
  return {
    roomCode: null,
    opponent: { id: SOLO_CLOCK, name: 'Clock' },
    youAre: SOLO_YOU,
    bestOf: SOLO_BEST_OF,
    roundsToWin: SOLO_ROUNDS_TO_WIN,
  };
}

export function roundStartPayload(r: SoloRound): RoundStartPayload {
  return {
    index: r.index,
    digits: r.digits,
    target: r.target,
    timeLimitMs: r.timeLimitMs,
    endsAt: r.startedAt + r.timeLimitMs,
    difficultyCoefficient: r.difficultyCoefficient,
  };
}

export function roundEndPayload(
  r: SoloRound,
  winnerId: string,
  winningExpression: string | null,
  scores: Record<string, number>,
  wins: Record<string, number>,
): RoundEndPayload {
  return {
    index: r.index,
    winnerId,
    winningExpression,
    sampleSolution: r.solution,
    scoresByPlayer: { ...scores },
    winsByPlayer: { ...wins },
  };
}

export function matchEndPayload(
  winnerId: string | null,
  scores: Record<string, number>,
  wins: Record<string, number>,
): MatchEndPayload {
  return {
    winnerId,
    reason: 'rounds',
    scoresByPlayer: { ...scores },
    winsByPlayer: { ...wins },
  };
}

export function emptyScores(): Record<string, number> {
  return { [SOLO_YOU]: 0, [SOLO_CLOCK]: 0 };
}
