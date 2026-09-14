import { describe, it, expect } from 'vitest';
import { validateExpression } from '@plusduel/shared';

const v = (expr: string, digits: number[], target: number) => validateExpression(expr, digits, target);

describe('basic arithmetic', () => {
  it('accepts the canonical example 5*(3+2)=25', () => {
    expect(v('5*(3+2)', [2, 3, 5], 25).valid).toBe(true);
  });

  it('accepts unicode operators × ÷ −', () => {
    expect(v('5 × (3 + 2)', [2, 3, 5], 25).valid).toBe(true);
    expect(v('6 ÷ 2 + 1', [6, 2, 1], 4).valid).toBe(true);
    expect(v('10 − 3 − 2', [1, 0, 3, 2], 5).valid).toBe(true);
  });

  it('rejects wrong target value', () => {
    const r = v('5*(3+2)', [2, 3, 5], 26);
    expect(r.valid).toBe(false);
    expect(r.reason).toMatch(/26/);
  });

  it('rejects non-integer division results', () => {
    expect(v('7/2+1', [7, 2, 1], 4).valid).toBe(false);
    expect(v('3/2', [3, 2], 1).valid).toBe(false);
  });

  it('accepts exact division', () => {
    expect(v('8/4+1', [8, 4, 1], 3).valid).toBe(true);
  });

  it('rejects division by zero', () => {
    expect(v('5/0+3', [5, 0, 3], 3).valid).toBe(false);
  });
});

describe('digit concatenation', () => {
  it('accepts concatenated literals', () => {
    expect(v('23+2', [2, 3, 2], 25).valid).toBe(true);
    expect(v('12*3', [1, 2, 3], 36).valid).toBe(true);
  });

  it('allows reordering via concatenation', () => {
    expect(v('32+1', [2, 3, 1], 33).valid).toBe(true);
  });

  it('rejects concatenation that uses a digit not in the set', () => {
    expect(v('23+2', [2, 3], 25).valid).toBe(false);
  });

  it('rejects leading zero literals like "03"', () => {
    const r = v('03+5', [0, 3, 5], 8);
    expect(r.valid).toBe(false);
    expect(r.reason).toMatch(/[Ll]eading zero/);
  });

  it('rejects "0" glued to nonzero even mid-expression', () => {
    expect(v('(05)*2', [0, 5, 2], 10).valid).toBe(false);
  });

  it('accepts standalone zero digit', () => {
    expect(v('0+7', [0, 7], 7).valid).toBe(true);
  });
});

describe('digit multiset enforcement', () => {
  it('rejects digit reuse', () => {
    const r = v('22+1', [2, 1], 5);
    expect(r.valid).toBe(false);
  });

  it('accepts when multiset matches including duplicates', () => {
    expect(v('22+1', [2, 2, 1], 23).valid).toBe(true);
    expect(v('2^2+1', [2, 2, 1], 5).valid).toBe(true);
  });

  it('rejects unused digits', () => {
    expect(v('5*5', [5, 5, 3], 25).valid).toBe(false);
  });

  it('rejects digits outside the issued set', () => {
    // uses a 9 that was never issued
    expect(v('5*(3+2)+9', [5, 3, 2], 34).valid).toBe(false);
    // valid control: each issued digit exactly once
    expect(v('5*(3+2)+9', [5, 3, 2, 9], 34).valid).toBe(true);
  });

  it('rejects foreign characters / injection', () => {
    expect(v('import', [1, 2], 3).valid).toBe(false);
    expect(v('5;process.exit(1)', [5], 5).valid).toBe(false);
    expect(v('sqrt(9)', [9], 3).valid).toBe(false);
    expect(v('5*(3+a)', [5, 3, 2], 40).valid).toBe(false);
  });

  it('rejects unsupported functions even with valid digits', () => {
    expect(v('nthRoot(8,3)', [8, 3], 2).valid).toBe(false);
  });
});

describe('square root exactness', () => {
  it('accepts perfect squares', () => {
    expect(v('√9+1', [9, 1], 4).valid).toBe(true);
    expect(v('√16*2', [1, 6, 2], 8).valid).toBe(true);
    expect(v('√(16)*2', [1, 6, 2], 8).valid).toBe(true);
    expect(v('√(25)+√9', [2, 5, 9], 8).valid).toBe(true);
  });

  it('rejects irrational roots', () => {
    const r = v('√8+1', [8, 1], 3);
    expect(r.valid).toBe(false);
    expect(r.reason).toMatch(/non-exact|unsupported/i);
  });

  it('rejects sqrt of negative', () => {
    expect(v('√(0-9)', [0, 9], 3).valid).toBe(false);
  });
});

describe('factorial exactness', () => {
  it('accepts integer factorials', () => {
    expect(v('3!+1', [3, 1], 7).valid).toBe(true);
    expect(v('5!-2^4', [5, 2, 4], 104).valid).toBe(true);
  });

  it('rejects factorial of non-integer', () => {
    expect(v('(7/2)!', [7, 2], 3).valid).toBe(false);
  });

  it('rejects factorial of negative', () => {
    expect(v('(0-3)!', [0, 3], 1).valid).toBe(false);
  });

  it('caps absurd factorials', () => {
    expect(v('(5^3)!', [5, 3], 1).valid).toBe(false);
  });
});

describe('exponentiation', () => {
  it('accepts integer exponents', () => {
    expect(v('2^3+1', [2, 3, 1], 9).valid).toBe(true);
  });

  it('rejects irrational results', () => {
    expect(v('2^(1/2)', [2, 1, 2], 1).valid).toBe(false);
  });

  it('rejects negative exponent producing fraction', () => {
    // 2^-1 = 1/2 not an integer
    expect(v('2^(0-1)', [2, 0, 1], 0).valid).toBe(false);
  });
});

describe('unary minus and parens', () => {
  it('supports unary minus', () => {
    expect(v('-2+5', [2, 5], 3).valid).toBe(true);
  });

  it('rejects unbalanced parens', () => {
    expect(v('(5+2', [5, 2], 7).valid).toBe(false);
  });
});
