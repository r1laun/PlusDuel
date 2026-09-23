import { useEffect, useState } from 'react';
import type { ValidationResult } from '@plusduel/shared';

/**
 * Instant client-side validation (tier 1). The server re-validates (tier 2).
 * Validator module is lazy-imported to keep initial bundle light.
 */
export function useLocalValidation(
  expr: string,
  digits: number[],
  target: number,
  active: boolean,
): { valid: boolean; reason?: string; checked: boolean } {
  const [result, setResult] = useState<{ valid: boolean; reason?: string; checked: boolean }>({
    valid: false,
    checked: false,
  });

  useEffect(() => {
    if (!active || !expr) {
      setResult({ valid: false, checked: false });
      return;
    }
    let cancelled = false;
    import('@plusduel/shared/validator').then(({ validateExpression }) => {
      if (cancelled) return;
      const r = validateExpression(expr, digits, target);
      setResult({ valid: r.valid, reason: r.reason, checked: true });
    });
    return () => {
      cancelled = true;
    };
  }, [expr, digits, target, active]);

  return result;
}
