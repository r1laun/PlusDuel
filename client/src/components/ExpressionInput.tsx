import { useMemo } from 'react';
import type { DragEvent } from 'react';

interface ValidationState {
  valid: boolean;
  reason?: string;
  checked: boolean;
}

interface Props {
  expr: string;
  setExpr: (updater: (prev: string) => string) => void;
  digits: number[];
  symbols: string[];
  isTouch: boolean;
  disabled: boolean;
  validation: ValidationState;
  onSubmit: () => void;
}

/** The digit row always renders 5 slots; rounds with fewer digits pad with empties. */
const DIGIT_SLOTS = 5;

/** Split expression into display tokens: digit-runs stay together, each op is its own chip. */
function tokenize(expr: string): { text: string; isDigitRun: boolean }[] {
  const tokens: { text: string; isDigitRun: boolean }[] = [];
  let i = 0;
  while (i < expr.length) {
    const ch = expr[i]!;
    if (ch >= '0' && ch <= '9') {
      let j = i;
      while (j < expr.length && expr[j]! >= '0' && expr[j]! <= '9') j++;
      tokens.push({ text: expr.slice(i, j), isDigitRun: true });
      i = j;
    } else {
      tokens.push({ text: ch, isDigitRun: false });
      i++;
    }
  }
  return tokens;
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
  const tokens = useMemo(() => tokenize(expr), [expr]);

  const append = (text: string) => setExpr((prev) => prev + text);
  const backspace = () => setExpr((prev) => prev.slice(0, -1));
  const clear = () => setExpr(() => '');

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const text = e.dataTransfer.getData('text/plain');
    if (text) append(text);
  };

  const keyClass = disabled ? 'pd-key pd-key--disabled' : 'pd-key';

  return (
    <div>
      {!isTouch && (
        <input
          className="pd-input"
          value={expr}
          onChange={(e) => setExpr(() => e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && validation.valid && !disabled) onSubmit();
          }}
          placeholder="e.g. 5×(3+2)"
          disabled={disabled}
          autoFocus
          spellCheck={false}
          autoComplete="off"
          aria-label="Expression"
        />
      )}
      <div
        className={`pd-info-box${validation.checked && validation.valid ? ' pd-info-box--accent' : ''}`}
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDrop}
      >
        {tokens.length === 0 ? (
          <span>{isTouch ? 'Tap tiles to build your answer' : 'Use each digit once'}</span>
        ) : (
          tokens.map((t, i) => <span key={i}>{pretty(t.text)}</span>)
        )}
      </div>

      <div className={`pd-status${validation.checked && validation.valid ? ' pd-status--accent' : ''}`}>
        {validation.checked && (validation.valid ? 'Valid — ready to submit' : validation.reason ?? '')}
      </div>

      <div>
        <div className="pd-keypad" role="group" aria-label="operators">
          {symbols.map((s) => (
            <button
              key={s}
              className={keyClass}
              disabled={disabled}
              draggable={!disabled}
              onDragStart={(e) => e.dataTransfer.setData('text/plain', s)}
              onClick={() => !disabled && append(s)}
            >
              {s}
            </button>
          ))}
          <button
            className={keyClass}
            onClick={backspace}
            disabled={disabled || !expr}
            aria-label="Backspace"
          >
            ⌫
          </button>
          <button className={keyClass} onClick={clear} disabled={disabled || !expr}>
            Clear
          </button>
          <button
            className={`pd-key pd-key--confirm${disabled || !validation.valid ? ' pd-key--disabled' : ''}`}
            onClick={onSubmit}
            disabled={disabled || !validation.valid}
          >
            Submit
          </button>
        </div>

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
            return (
              <button
                key={`${i}-${d}`}
                className={disabled ? 'pd-key pd-key--active pd-key--disabled' : 'pd-key pd-key--active'}
                disabled={disabled}
                draggable={!disabled}
                onDragStart={(e) => e.dataTransfer.setData('text/plain', String(d))}
                onClick={() => !disabled && append(String(d))}
              >
                {d}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function pretty(text: string): string {
  return text.replace(/\*/g, '×').replace(/\//g, '÷').replace(/-/g, '−').replace(/sqrt/g, '√');
}
