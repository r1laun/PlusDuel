export interface LevelConfig {
  level: number;
  minDigits: number;
  maxDigits: number;
  targetMin: number;
  targetMax: number;
  timeLimitSec: number;
}

export const LEVELS: LevelConfig[] = [
  { level: 1, minDigits: 3, maxDigits: 4, targetMin: 1, targetMax: 100, timeLimitSec: 60 },
  { level: 2, minDigits: 4, maxDigits: 5, targetMin: 1, targetMax: 200, timeLimitSec: 75 },
  { level: 3, minDigits: 5, maxDigits: 5, targetMin: 1, targetMax: 400, timeLimitSec: 90 },
  { level: 4, minDigits: 5, maxDigits: 5, targetMin: 10, targetMax: 800, timeLimitSec: 90 },
  { level: 5, minDigits: 5, maxDigits: 5, targetMin: 20, targetMax: 999, timeLimitSec: 75 },
  { level: 6, minDigits: 5, maxDigits: 5, targetMin: 50, targetMax: 999, timeLimitSec: 60 },
];

export const MAX_LEVEL = LEVELS.length;

export function configForLevel(level: number): LevelConfig {
  const clamped = Math.max(1, Math.min(MAX_LEVEL, Math.round(level)));
  return LEVELS[clamped - 1]!;
}

/**
 * Difficulty coefficient derived from digit count + target range.
 * score = speed_factor x difficulty_coefficient
 */
export function difficultyCoefficient(digitCount: number, targetMax: number): number {
  const rangeBand = Math.max(1, Math.ceil(targetMax / 100));
  return round1(digitCount * rangeBand);
}

/** speed_factor in [0.25, 1]: fraction of round time remaining at submit. */
export function speedFactor(remainingMs: number, timeLimitMs: number): number {
  if (timeLimitMs <= 0) return 0.25;
  return Math.max(0.25, Math.min(1, remainingMs / timeLimitMs));
}

export function roundScore(
  digitCount: number,
  targetMax: number,
  remainingMs: number,
  timeLimitMs: number,
): number {
  return round1(difficultyCoefficient(digitCount, targetMax) * speedFactor(remainingMs, timeLimitMs));
}

function round1(n: number): number {
  return Math.round(n * 10) / 10;
}