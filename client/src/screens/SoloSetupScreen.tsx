import { useState } from 'react';
import { LEVELS } from '@plusduel/shared';
import type { SoloMode } from '../solo/engine';

interface Props {
  onStart: (mode: SoloMode, level: number) => void;
  onBack: () => void;
}

export default function SoloSetupScreen({ onStart, onBack }: Props) {
  const [mode, setMode] = useState<SoloMode>('endless');
  const [level, setLevel] = useState(1);

  const cfg = LEVELS[level - 1] ?? LEVELS[0]!;

  return (
    <div className="pd-frame">
      <p className="pd-status">Practice solo — no opponent, just you and the numbers.</p>

      <div className="pd-panel">
        <div className="pd-row">
          <button
            className={`pd-btn pd-btn--outline${mode === 'endless' ? ' pd-btn--active' : ''}`}
            onClick={() => setMode('endless')}
          >
            Training
          </button>
          <button
            className={`pd-btn pd-btn--outline${mode === 'match' ? ' pd-btn--active' : ''}`}
            onClick={() => setMode('match')}
          >
            Match
          </button>
        </div>
        <p className="pd-status">
          {mode === 'endless'
            ? 'Endless rounds — solve, learn the solution, repeat.'
            : 'Best of 5 against the Clock — 3 round wins take the match.'}
        </p>
      </div>

      <div className="pd-panel">
        <p className="pd-status">Level</p>
        <div className="pd-level-grid">
          {LEVELS.map((l) => (
            <button
              key={l.level}
              className={`pd-key${l.level === level ? ' pd-key--active' : ''}`}
              onClick={() => setLevel(l.level)}
            >
              {l.level}
            </button>
          ))}
        </div>
        <p className="pd-status">
          {cfg.minDigits}–{cfg.maxDigits} digits · target {cfg.targetMin}–{cfg.targetMax} · {cfg.timeLimitSec}s
        </p>
      </div>

      <button className="pd-btn pd-btn--primary" onClick={() => onStart(mode, level)}>
        Start
      </button>
      <button className="pd-btn pd-btn--outline" onClick={onBack}>
        Back
      </button>
    </div>
  );
}
