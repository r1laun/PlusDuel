import Fraction from 'fraction.js';
import { configForLevel } from './difficulty.js';

export interface GeneratedRound {
  digits: number[];
  target: number;
  solution: string;
}

interface Item {
  value: Fraction;
  expr: string;
}

function randInt(min: number, max: number): number {
  return min + Math.floor(Math.random() * (max - min + 1));
}

/** All ways to split the digit sequence into consecutive multi-digit literals. */
function compositions(digits: number[]): number[][][] {
  const n = digits.length;
  const masks: number[][][] = [];
  for (let mask = 0; mask < 1 << (n - 1); mask++) {
    const groups: number[][] = [];
    let current: number[] = [digits[0]!];
    for (let i = 1; i < n; i++) {
      if (mask & (1 << (i - 1))) {
        groups.push(current);
        current = [digits[i]!];
      } else {
        current.push(digits[i]!);
      }
    }
    groups.push(current);
    masks.push(groups);
  }
  return masks;
}

function groupValue(group: number[]): { value: number; expr: string } | null {
  if (group.length > 1 && group[0] === 0) return null; // leading zero
  return { value: Number(group.join('')), expr: group.join('') };
}

const MAX_DIGITS_OF_RESULT = 25;

function safeLen(f: Fraction): boolean {
  return Math.max(f.n.toString().length, f.d.toString().length) <= MAX_DIGITS_OF_RESULT;
}

function combine(a: Item, b: Item): Item[] {
  const out: Item[] = [];
  const x = a.value;
  const y = b.value;

  try {
    const sum = x.add(y);
    if (safeLen(sum)) out.push({ value: sum, expr: `(${a.expr}+${b.expr})` });

    const prod = x.mul(y);
    if (safeLen(prod)) out.push({ value: prod, expr: `(${a.expr}*${b.expr})` });

    const div = x.div(y);
    if (safeLen(div)) out.push({ value: div, expr: `(${a.expr}/${b.expr})` });
    const rdiv = y.div(x);
    if (safeLen(rdiv)) out.push({ value: rdiv, expr: `(${b.expr}/${a.expr})` });

    const sub = x.sub(y);
    if (safeLen(sub)) out.push({ value: sub, expr: `(${a.expr}-${b.expr})` });
    const rsub = y.sub(x);
    if (safeLen(rsub)) out.push({ value: rsub, expr: `(${b.expr}-${a.expr})` });

    // small integer powers both ways
    for (const [base, e, expr] of [
      [x, y, `${a.expr}^${b.expr}`],
      [y, x, `${b.expr}^${a.expr}`],
    ] as const) {
      if (e.d === 1n && e.n >= 0n && e.n <= 6n && base.d === 1n && base.n <= 100n && base.n >= -100n) {
        const v = base.pow(Number(e.n));
        if (safeLen(v)) out.push({ value: v, expr: `(${expr})` });
      }
    }
  } catch {
    /* overflow in fraction lib — skip */
  }

  return out;
}

/**
 * Find an expression over exactly the given digits evaluating to target.
 * Exported for tests / smoke tooling; generator uses it to guarantee solvable rounds.
 */
export function findSolution(digits: number[], target: number): string | null {
  const tf = new Fraction(target);
  const budget = { nodes: 200_000 };
  for (const r of search(digits, Infinity, budget)) {
    if (r.value.equals(tf) && r.value.d === 1n) return r.expr;
  }
  return null;
}

/**
 * Depth-first combined search over all compositions + pair merges.
 * Iterates unordered pairs only (i<j) and lets `combine` emit both orientations
 * of non-commutative operators, so symmetric work is avoided.
 * `collectLimit` stops once enough distinct integer results are found;
 * `budget.nodes` bounds worst-case exploration so pathological multisets can't hang.
 */
function search(
  digits: number[],
  collectLimit: number,
  budget: { nodes: number },
): Item[] {
  const found: Item[] = [];
  const seenValues = new Set<string>();
  const collect = (v: Item) => {
    if (budget.nodes-- < 0) return;
    const key = v.value.toString();
    if (!seenValues.has(key)) {
      seenValues.add(key);
      if (v.value.d === 1n) found.push(v);
    }
  };

  const go = (items: Item[]) => {
    if (budget.nodes < 0 || found.length >= collectLimit) return;
    if (items.length === 1) {
      collect(items[0]!);
      return;
    }
    for (let i = 0; i < items.length; i++) {
      for (let j = i + 1; j < items.length; j++) {
        const rest: Item[] = [];
        for (let k = 0; k < items.length; k++) {
          if (k !== i && k !== j) rest.push(items[k]!);
        }
        for (const combo of combine(items[i]!, items[j]!)) {
          go([...rest, combo]);
          if (found.length >= collectLimit || budget.nodes < 0) return;
        }
      }
    }
  };

  for (const groups of compositions(digits)) {
    const leaves: Item[] = [];
    let valid = true;
    for (const g of groups) {
      const gv = groupValue(g);
      if (!gv) {
        valid = false;
        break;
      }
      leaves.push({ value: new Fraction(gv.value), expr: gv.expr });
    }
    if (valid) go(leaves);
    if (found.length >= collectLimit || budget.nodes < 0) break;
  }

  return found;
}

function enumerateComposedExact(digits: number[], targetMin: number, targetMax: number): Item[] {
  const budget = { nodes: 120_000 };
  return search(digits, 300, budget).filter((r) => {
    const v = Number(r.value.s * r.value.n);
    return v >= targetMin && v <= targetMax;
  });
}

/**
 * Generate a guaranteed-solvable round for a level:
 * sample digits, enumerate reachable exact-integer targets in range, pick one.
 * Falls back to smaller digit counts if a level's full size is unruly.
 */
export function generateRound(level: number): GeneratedRound {
  let cfg = configForLevel(level);

  for (let attempt = 0; attempt < 40; attempt++) {
    const count = randInt(cfg.minDigits, cfg.maxDigits);
    const digits: number[] = [randInt(1, 9)];
    for (let i = 1; i < count; i++) digits.push(randInt(0, 9));

    const candidates = enumerateComposedExact(digits, cfg.targetMin, cfg.targetMax);
    if (candidates.length === 0) continue;

    const pick = candidates[Math.floor(Math.random() * candidates.length)]!;
    const expr = pick.expr.replace(/^\((.*)\)$/, '$1');
    return {
      digits,
      target: Number(pick.value.s * pick.value.n),
      solution: expr,
    };
  }

  // If truncated search found nothing, retry with a smaller digit count.
  for (const fallbackLevel of [Math.max(1, cfg.level - 1), 1]) {
    cfg = configForLevel(fallbackLevel);
    for (let attempt = 0; attempt < 20; attempt++) {
      const count = randInt(cfg.minDigits, cfg.maxDigits);
      const digits: number[] = [randInt(1, 9)];
      for (let i = 1; i < count; i++) digits.push(randInt(0, 9));
      const candidates = enumerateComposedExact(digits, cfg.targetMin, cfg.targetMax);
      if (candidates.length > 0) {
        const pick = candidates[Math.floor(Math.random() * candidates.length)]!;
        const expr = pick.expr.replace(/^\((.*)\)$/, '$1');
        return {
          digits,
          target: Number(pick.value.s * pick.value.n),
          solution: expr,
        };
      }
    }
  }

  // Deterministic last-resort — always solvable.
  return { digits: [2, 3, 5], target: 25, solution: '5*(3+2)' };
}