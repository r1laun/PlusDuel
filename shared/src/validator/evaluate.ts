import Fraction from 'fraction.js';

type Big = bigint | number;

export function toFraction(x: Fraction | Big): Fraction {
  return x instanceof Fraction ? x : new Fraction(x.toString());
}

export function isExactInteger(f: Fraction): boolean {
  return f.d === 1n || Number(f.d) === 1;
}

/** Integer value as JS number; caller must have checked isExactInteger. */
export function intValue(f: Fraction): number {
  const n = typeof f.n === 'bigint' ? f.n : BigInt(f.n);
  return Number(f.s * n);
}

function bigStrLen(f: Fraction): number {
  return Math.max(f.n.toString().length, f.d.toString().length);
}

const MAX_MAGNITUDE_DIGITS = 30;
const MAX_FACTORIAL = 200;
const MAX_EXPONENT = 32;

function guardOverflow(f: Fraction): Fraction | null {
  return bigStrLen(f) > MAX_MAGNITUDE_DIGITS ? null : f;
}

function factorialBig(n: number): bigint {
  let acc = 1n;
  for (let i = 2n; i <= BigInt(n); i++) acc *= i;
  return acc;
}

function isqrt(n: bigint): bigint {
  if (n < 2n) return n;
  let x = BigInt(Math.floor(Math.sqrt(Number(n)))) + 1n;
  let y = (x + n / x) >> 1n;
  while (y < x) {
    x = y;
    y = (x + n / x) >> 1n;
  }
  return x;
}

/**
 * Exact rational evaluation of a math.js AST using fraction.js.
 * Returns null for anything that cannot be computed exactly
 * (non-integer intermediates, irrational sqrt, overflow, division by zero).
 */
export function evaluateExact(node: unknown): Fraction | null {
  const n = node as { type?: string } & Record<string, any>;
  if (!n || typeof n.type !== 'string') return null;

  switch (n.type) {
    case 'ConstantNode': {
      const v = n.value;
      if (typeof v !== 'number' || !Number.isSafeInteger(v)) return null;
      return new Fraction(v);
    }

    case 'ParenthesisNode':
      return evaluateExact(n.content);

    case 'OperatorNode':
      return evalOperator(n);

    case 'FunctionNode': {
      const name = String(n.fn?.name ?? '');
      if (name !== 'sqrt') return null;
      if (!Array.isArray(n.args) || n.args.length !== 1) return null;
      const arg = evaluateExact(n.args[0]);
      if (!arg || !isExactInteger(arg)) return null;
      const val = BigInt(intValue(arg));
      if (val < 0n) return null;
      const r = isqrt(val);
      if (r * r !== val) return null; // sqrt must be exact integer
      return guardOverflow(new Fraction(r.toString()));
    }

    default:
      return null;
  }
}

function evalOperator(n: any): Fraction | null {
  const fn = String(n.fn ?? '');
  const args: unknown[] = Array.isArray(n.args) ? n.args : [];
  const implicit = n.implicit === true;

  if (fn === 'unaryMinus') {
    if (args.length !== 1) return null;
    const a = evaluateExact(args[0]);
    if (!a) return null;
    return guardOverflow(toFraction(a).mul(new Fraction(-1)));
  }

  if (fn === 'factorial' && !implicit) {
    if (args.length !== 1) return null;
    const a = evaluateExact(args[0]);
    if (!a || !isExactInteger(a)) return null;
    const v = intValue(a);
    if (v < 0 || v > MAX_FACTORIAL) return null;
    return guardOverflow(new Fraction(factorialBig(v).toString()));
  }

  if (args.length !== 2) return null;
  const a = evaluateExact(args[0]);
  const b = evaluateExact(args[1]);
  if (!a || !b) return null;
  const fa = toFraction(a);
  const fb = toFraction(b);

  switch (fn) {
    case '+':
    case 'add':
      return guardOverflow(fa.add(fb));
    case '-':
    case 'subtract':
      return guardOverflow(fa.sub(fb));
    case '*':
    case 'multiply':
      return guardOverflow(fa.mul(fb));
    case '/':
    case 'divide':
      if (fb.n === 0n || Number(fb.n) === 0) return null;
      return guardOverflow(fa.div(fb));
    case '^':
    case 'pow': {
      if (!isExactInteger(fb)) return null;
      const exp = intValue(fb);
      if (Math.abs(exp) > MAX_EXPONENT) return null;
      if ((fa.n === 0n || Number(fa.n) === 0) && exp < 0) return null;
      try {
        return guardOverflow(fa.pow(exp));
      } catch {
        return null;
      }
    }
    default:
      return null;
  }
}
