import { useEffect, useMemo, useRef, useState } from 'react';
import type { DragEvent } from 'react';
import type { LocalValidationState } from '../hooks/useLocalValidation';
import { useLang } from '../i18n';
import { playDelete, playKey, playMistake, playSubmit } from '../sound/click';

interface Props {
  expr: string;
  setExpr: (updater: (prev: string) => string) => void;
  digits: number[];
  symbols: string[];
  isTouch: boolean;
  disabled: boolean;
  validation: LocalValidationState;
  onSubmit: () => void;
}

interface CharEntry {
  ch: string;
  slotIndex: number | null;
}

/** The digit row always renders 5 slots; rounds with fewer digits pad with empties. */
const DIGIT_SLOTS = 5;

/**
 * Reconciles a raw expression string with the issued digit slots so both
 * tile taps and desktop keyboard edits accurately highlight used digit slots.
 */
function reconcileEntries(
  expr: string,
  digits: number[],
  prevEntries: CharEntry[],
): CharEntry[] {
  const prevStr = prevEntries.map((e) => e.ch).join('');
  if (prevStr === expr) return prevEntries;

  const usedSlots = new Set<number>();
  const out: CharEntry[] = [];

  for (let i = 0; i < expr.length; i++) {
    const ch = expr[i]!;
    if (ch >= '0' && ch <= '9') {
      const num = Number(ch);
      const prevAtPos = prevEntries[i];
      if (
        prevAtPos &&
        prevAtPos.ch === ch &&
        prevAtPos.slotIndex !== null &&
        digits[prevAtPos.slotIndex] === num &&
        !usedSlots.has(prevAtPos.slotIndex)
      ) {
        usedSlots.add(prevAtPos.slotIndex);
        out.push({ ch, slotIndex: prevAtPos.slotIndex });
        continue;
      }
      let matchedSlot: number | null = null;
      for (let s = 0; s < digits.length; s++) {
        if (!usedSlots.has(s) && digits[s] === num) {
          matchedSlot = s;
          usedSlots.add(s);
          break;
        }
      }
      out.push({ ch, slotIndex: matchedSlot });
    } else {
      out.push({ ch, slotIndex: null });
    }
  }
  return out;
}

export default function ExpressionInput({
  expr,
  setExpr,
  digits,
  symbols,
  isTouch,
  disabled,
  validation,
  onSubmit,
}: Props) {
  const { t } = useLang();
  const entriesRef = useRef<CharEntry[]>([]);
  const [shaking, setShaking] = useState(false);

  const entries = useMemo(() => {
    const next = reconcileEntries(expr, digits, entriesRef.current);
    entriesRef.current = next;
    return next;
  }, [expr, digits]);

  useEffect(() => {
    if (!expr) entriesRef.current = [];
  }, [expr]);

  const usedSlotIndices = useMemo(() => {
    const set = new Set<number>();
    for (const e of entries) {
      if (e.slotIndex !== null) set.add(e.slotIndex);
    }
    return set;
  }, [entries]);

  const triggerShake = () => {
    setShaking(false);
    requestAnimationFrame(() => setShaking(true));
  };

  const commitEntries = (next: CharEntry[]) => {
    entriesRef.current = next;
    const str = next.map((e) => e.ch).join('');
    setExpr(() => str);
  };

  const handleDigitSlotTap = (slotIndex: number, digit: number) => {
    if (disabled) return;
    if (!usedSlotIndices.has(slotIndex)) {
      playKey();
      commitEntries([...entries, { ch: String(digit), slotIndex }]);
    } else {
      // PlusMath smart behavior: tapping an already-used digit slot removes it
      let lastIdx = -1;
      for (let i = entries.length - 1; i >= 0; i--) {
        if (entries[i]!.slotIndex === slotIndex) {
          lastIdx = i;
          break;
        }
      }
      if (lastIdx !== -1) {
        playDelete();
        const next = entries.slice(0, lastIdx).concat(entries.slice(lastIdx + 1));
        commitEntries(next);
      }
    }
  };

  const appendOp = (op: string) => {
    if (disabled) return;
    playKey();
    commitEntries([...entries, { ch: op, slotIndex: null }]);
  };

  const backspace = () => {
    if (disabled || entries.length === 0) return;
    playDelete();
    commitEntries(entries.slice(0, -1));
  };

  const clear = () => {
    if (disabled || entries.length === 0) return;
    playDelete();
    commitEntries([]);
  };

  const submit = () => {
    if (disabled || !expr.trim()) return;
    if (!validation.valid) {
      playMistake();
      triggerShake();
      return;
    }
    playSubmit();
    onSubmit();
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const text = e.dataTransfer.getData('text/plain');
    if (text) {
      playKey();
      setExpr((prev) => prev + text);
    }
  };

  const keyClass = disabled ? 'pd-key pd-key--disabled' : 'pd-key';
  const showErrorBanner =
    validation.checked &&
    !validation.valid &&
    (validation.extraDigits.length > 0 ||
      validation.allDigitsUsedExact ||
      (validation.reason && /Leading zero|Invalid character/i.test(validation.reason)));

  return (
    <div className="pd-keypad-wrap">
      {!isTouch && (
        <input
          className="pd-input"
          value={expr}
          onChange={(e) => setExpr(() => e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !disabled && expr.trim()) {
              e.preventDefault();
              submit();
            }
          }}
          placeholder={t('exprPh')}
          disabled={disabled}
          autoFocus
          spellCheck={false}
          autoComplete="off"
          aria-label={t('exprPh')}
        />
      )}

      {/* Fixed-height Status / Feedback Slot (from PlusMath GameScreen) */}
      <div
        className={`pd-status-banner${
          validation.valid
            ? ' pd-status-banner--solved'
            : showErrorBanner
              ? ' pd-status-banner--error'
              : ''
        }`}
      >
        {validation.valid
          ? t('readyToSubmit')
          : showErrorBanner
            ? (validation.reason ?? '')
            : t('operatorsHint')}
      </div>

      {/* Expression Display Box with Live Intermediate Calculation Pill */}
      <div
        className={`pd-expr-box${validation.valid ? ' pd-expr-box--solved' : ''}${shaking ? ' pd-shake' : ''}`}
        onAnimationEnd={() => setShaking(false)}
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDrop}
      >
        {expr.length === 0 ? (
          <span className="pd-expr-box__placeholder">
            {isTouch ? t('hintTouch') : t('hintDesktop')}
          </span>
        ) : (
          <span className="pd-expr-box__text">{pretty(expr)}</span>
        )}

        {validation.liveDisplay !== null && (
          <span className={`pd-live-pill${validation.valid ? ' pd-live-pill--solved' : ''}`}>
            = {validation.liveDisplay}
          </span>
        )}
      </div>

      {/* Smart 5-Slot Digit Row */}
      <div className="pd-digit-row" role="group" aria-label="digits">
        {Array.from({ length: DIGIT_SLOTS }, (_, i) => {
          const d = digits[i];
          if (d === undefined) {
            return (
              <button
                key={`empty-${i}`}
                className="pd-key pd-key--empty"
                disabled
                tabIndex={-1}
                aria-hidden="true"
              />
            );
          }
          const isUsed = usedSlotIndices.has(i);
          return (
            <button
              key={`${i}-${d}`}
              className={`${disabled ? 'pd-key pd-key--disabled' : 'pd-key'}${isUsed ? ' pd-key--used' : ''}`}
              disabled={disabled}
              draggable={!disabled}
              onDragStart={(e) => e.dataTransfer.setData('text/plain', String(d))}
              onClick={() => handleDigitSlotTap(i, d)}
            >
              <span>{d}</span>
              {isUsed && (
                <span className="pd-key__check" aria-hidden="true">
                  ✓
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* 3x3 Symmetric Operator Keypad */}
      <div className="pd-keypad" role="group" aria-label="operators">
        {symbols.map((s) => (
          <button
            key={s}
            className={keyClass}
            disabled={disabled}
            draggable={!disabled}
            onDragStart={(e) => e.dataTransfer.setData('text/plain', s)}
            onClick={() => appendOp(s)}
          >
            {s}
          </button>
        ))}
      </div>

      {/* Actions Row: Backspace / Clear / Submit */}
      <div className="pd-actions" role="group" aria-label="actions">
        <button
          className={keyClass}
          onClick={backspace}
          disabled={disabled || !expr}
          aria-label="Backspace"
        >
          ⌫
        </button>
        <button className={keyClass} onClick={clear} disabled={disabled || !expr}>
          {t('clear')}
        </button>
        <button
          className={`pd-key pd-key--confirm${
            validation.valid ? ' pd-key--confirm-ready' : ''
          }${disabled || !expr.trim() ? ' pd-key--disabled' : ''}`}
          onClick={submit}
          disabled={disabled || !expr.trim()}
        >
          {t('submit')}
        </button>
      </div>
    </div>
  );
}

function pretty(text: string): string {
  return text.replace(/\*/g, '×').replace(/\//g, '÷').replace(/-/g, '−').replace(/sqrt/g, '√');
}
