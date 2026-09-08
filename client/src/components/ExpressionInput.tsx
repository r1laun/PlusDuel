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

  return (
    <div className="input-area">
      {!isTouch && (
        <input
          className="text-input"
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
        />
      )}
      <div
        className={`expression-bar${validation.valid ? ' valid' : ''}${validation.checked && !validation.valid ? ' invalid' : ''}`}
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDrop}
      >
        {tokens.length === 0 ? (
          <span className="placeholder">{isTouch ? 'Tap tiles to build your answer' : 'Use each digit once'}</span>
        ) : (
          tokens.map((t, i) => (
            <span key={i} className={`token ${t.isDigitRun ? '' : 'op'}`}>
              {pretty(t.text)}
            </span>
          ))
        )}
      </div>

      <div className={`validation-line ${validation.checked ? (validation.valid ? 'ok' : 'bad') : ''}`}>
        {validation.checked && (validation.valid ? 'Valid — ready to submit' : validation.reason ?? '')}
      </div>

      <div className="pad">
        <div className="ops-row" role="group" aria-label="operators">
          {symbols.map((s) => (
            <button
              key={s}
              className="tile op"
              disabled={disabled}
              draggable={!disabled}
              onDragStart={(e) => e.dataTransfer.setData('text/plain', s)}
              onClick={() => !disabled && append(s)}
            >
              {s}
            </button>
          ))}
        </div>

        <div className="digits-row-pad" role="group" aria-label="digits">
          {digits.map((d, i) => (
            <button
              key={`${i}-${d}`}
              className="tile"
              disabled={disabled}
              draggable={!disabled}
              onDragStart={(e) => e.dataTransfer.setData('text/plain', String(d))}
              onClick={() => !disabled && append(String(d))}
            >
              {d}
            </button>
          ))}
        </div>

        <div className="actions">
          <button className="btn ghost" onClick={backspace} disabled={disabled || !expr}>
            ⌫
          </button>
          <button className="btn ghost" onClick={clear} disabled={disabled || !expr}>
            Clear
          </button>
          <button
            className="btn primary"
            onClick={onSubmit}
            disabled={disabled || !validation.valid}
          >
            Submit
          </button>
        </div>
      </div>
    </div>
  );
}

function pretty(text: string): string {
  return text.replace(/\*/g, '×').replace(/\//g, '÷').replace(/-/g, '−').replace(/sqrt/g, '√');
}