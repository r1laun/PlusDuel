import { describe, it, expect } from 'vitest';
import { configForLevel, generateRound, findSolution, validateExpression } from '@plusduel/shared';

describe('generateRound', () => {
  it('produces solvable rounds with a valid solution', () => {
    for (const level of [1, 2, 3, 4, 5, 6]) {
      const round = generateRound(level);
      const cfg = configForLevel(level);
      expect(round.digits.length).toBeLessThanOrEqual(6);
      expect(round.digits.length).toBeGreaterThanOrEqual(cfg.minDigits);
      const check = validateExpression(round.solution, round.digits, round.target);
      if (!check.valid) {
        throw new Error(
          `level ${level}: generated solution invalid: ${round.solution} = ${round.target} (${check.reason})`,
        );
      }
    }
  });

  it('respects target range for the level', () => {
    for (let i = 0; i < 20; i++) {
      const round = generateRound(1); // target 1..100
      expect(round.target).toBeGreaterThanOrEqual(1);
      expect(round.target).toBeLessThanOrEqual(100);
    }
  });

  it('is deterministic via findSolution', () => {
    const sol = findSolution([2, 3, 5], 25);
    expect(sol).not.toBeNull();
    expect(validateExpression(sol!, [2, 3, 5], 25).valid).toBe(true);
    expect(findSolution([7, 7], 999_999)).toBeNull();
  });
});
