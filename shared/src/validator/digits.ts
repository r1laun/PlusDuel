export interface DigitScanOk {
  ok: true;
  usedDigits: number[];
  normalized: string;
}

export interface DigitScanFail {
  ok: false;
  reason: string;
}

const ALLOWED_CHARS = /^[0-9+\-*/^!()\s√]+$/;

/**
 * Scan raw expression text:
 *  - reject any character outside the allowed set
 *  - group consecutive digit characters into literals; reject leading zeros ("03")
 *  - collect the multiset of digit characters used
 *  - normalize unicode operators and rewrite "√x" / "√(...)" into "sqrt(...)"
 */
export function scanExpression(input: string): DigitScanOk | DigitScanFail {
  let expr = input
    .replace(/−/g, '-')
    .replace(/×/g, '*')
    .replace(/÷/g, '/')
    .trim();

  if (!ALLOWED_CHARS.test(expr)) {
    return { ok: false, reason: 'Invalid character in expression' };
  }

  const usedDigits: number[] = [];
  let out = '';
  let i = 0;

  while (i < expr.length) {
    const ch = expr[i]!;

    if (ch >= '0' && ch <= '9') {
      let j = i;
      while (j < expr.length && expr[j]! >= '0' && expr[j]! <= '9') j++;
      const literal = expr.slice(i, j);
      if (literal.length > 1 && literal[0] === '0') {
        return { ok: false, reason: `Leading zero not allowed in "${literal}"` };
      }
      for (const c of literal) usedDigits.push(Number(c));
      out += literal;
      i = j;
      continue;
    }

    if (ch === '√') {
      let j = i + 1;
      while (j < expr.length && expr[j] === ' ') j++;
      if (j >= expr.length) return { ok: false, reason: '√ needs an argument' };

      const next = expr[j]!;
      if (next === '(') {
        let depth = 0;
        let k = j;
        for (; k < expr.length; k++) {
          if (expr[k] === '(') depth++;
          else if (expr[k] === ')') {
            depth--;
            if (depth === 0) break;
          }
        }
        if (k >= expr.length || depth !== 0) return { ok: false, reason: 'Unbalanced parentheses' };
        const inner = expr.slice(j + 1, k);
        for (const c of inner) if (c >= '0' && c <= '9') usedDigits.push(Number(c));
        out += `sqrt(${inner})`;
        i = k + 1;
        continue;
      }

      if (next >= '0' && next <= '9') {
        let k = j;
        while (k < expr.length && expr[k]! >= '0' && expr[k]! <= '9') k++;
        const literal = expr.slice(j, k);
        if (literal.length > 1 && literal[0] === '0') {
          return { ok: false, reason: `Leading zero not allowed in "${literal}"` };
        }
        for (const c of literal) usedDigits.push(Number(c));
        out += `sqrt(${literal})`;
        i = k;
        continue;
      }

      return { ok: false, reason: '√ needs a number or parenthesized expression' };
    }

    if (ch === ' ') {
      i++;
      continue;
    }

    out += ch;
    i++;
  }

  return { ok: true, usedDigits, normalized: out };
}

/** Compare two digit multisets. */
export function sameMultiset(a: number[], b: number[]): boolean {
  if (a.length !== b.length) return false;
  const sa = [...a].sort((x, y) => x - y);
  const sb = [...b].sort((x, y) => x - y);
  return sa.every((v, idx) => v === sb[idx]);
}
