import { useEffect, useState } from 'react';

export interface LocalValidationState {
  valid: boolean;
  reason?: string;
  checked: boolean;
  liveDisplay: string | null;
  usedDigits: number[];
  missingDigits: number[];
  extraDigits: number[];
  allDigitsUsedExact: boolean;
}

/**
 * Instant client-side validation & live expression preview (tier 1).
 * The server re-validates (tier 2).
 * Validator module is lazy-imported to keep initial bundle light.
 */
export function useLocalValidation(
  expr: string,
  digits: number[],
  target: number,
  active: boolean,
): LocalValidationState {
  const [result, setResult] = useState<LocalValidationState>({
    valid: false,
    checked: false,
    liveDisplay: null,
    usedDigits: [],
    missingDigits: digits,
    extraDigits: [],
    allDigitsUsedExact: false,
  });

  useEffect(() => {
    if (!active || !expr.trim()) {
      setResult({
        valid: false,
        checked: false,
        liveDisplay: null,
        usedDigits: [],
        missingDigits: digits,
        extraDigits: [],
        allDigitsUsedExact: false,
      });
      return;
    }
    let cancelled = false;
    import('@plusduel/shared/validator').then(({ previewExpression }) => {
      if (cancelled) return;
      const r = previewExpression(expr, digits, target);
      setResult({
        valid: r.valid,
        reason: r.reason,
        checked: true,
        liveDisplay: r.liveDisplay,
        usedDigits: r.usedDigits,
        missingDigits: r.missingDigits,
        extraDigits: r.extraDigits,
        allDigitsUsedExact: r.allDigitsUsedExact,
      });
    });
    return () => {
      cancelled = true;
    };
  }, [expr, digits, target, active]);

  return result;
}
