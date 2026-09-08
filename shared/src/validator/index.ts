import { parse } from 'mathjs';
import Fraction from 'fraction.js';
import { scanExpression, sameMultiset } from './digits.js';
import { evaluateExact, isExactInteger } from './evaluate.js';

export interface ValidationResult {
  valid: boolean;
  reason?: string;
  value?: number;
}

/**
 * Validate a player's expression for a round.
 * 1. Character/leading-zero/digit-multiset check via raw-text scan
 *    (math.js would silently normalize "03" to 3, so we must look at the text).
 * 2. Exact rational evaluation of the math.js AST (fraction.js, no floats).
 * 3. Result must be an exact integer equal to the target.
 */
export function validateExpression(
  expression: string,
  issuedDigits: number[],
  target: number,
): ValidationResult {
  if (!expression || typeof expression !== 'string') {
    return { valid: false, reason: 'Empty expression' };
  }

  const scan = scanExpression(expression);
  if (!scan.ok) return { valid: false, reason: scan.reason };

  if (!sameMultiset(scan.usedDigits, issuedDigits)) {
    const missing = multisetDiff(issuedDigits, scan.usedDigits);
    const extra = multisetDiff(scan.usedDigits, issuedDigits);
    const parts: string[] = [];
    if (missing.length) parts.push(`unused digit(s): ${missing.join(', ')}`);
    if (extra.length) parts.push(`digit(s) not in round set or reused: ${extra.join(', ')}`);
    return { valid: false, reason: parts.join(' — ') || 'Digits do not match round set' };
  }

  let ast;
  try {
    ast = parse(scan.normalized);
  } catch {
    return { valid: false, reason: 'Could not parse expression' };
  }

  let result;
  try {
    result = evaluateExact(ast);
  } catch {
    return { valid: false, reason: 'Evaluation failed' };
  }

  if (!result) {
    return { valid: false, reason: 'Expression has non-exact or unsupported intermediate values' };
  }

  if (!isExactInteger(result)) {
    return { valid: false, reason: 'Result is not an exact integer' };
  }

  const value = Number(result.s * result.n);
  if (value !== target) {
    return { valid: false, reason: `Evaluates to ${value}, target is ${target}` };
  }

  return { valid: true, value };
}

function multisetDiff(a: number[], b: number[]): number[] {
  const pool = [...b];
  const out: number[] = [];
  for (const v of a) {
    const idx = pool.indexOf(v);
    if (idx === -1) out.push(v);
    else pool.splice(idx, 1);
  }
  return out.sort((x, y) => x - y);
}

export { scanExpression, sameMultiset } from './digits.js';
export { evaluateExact, isExactInteger } from './evaluate.js';
export type { DigitScanFail, DigitScanOk } from './digits.js';
