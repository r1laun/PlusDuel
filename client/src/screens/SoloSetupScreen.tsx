import { useState } from 'react';
import { LEVELS } from '@plusduel/shared';
import type { SoloMode } from '../solo/engine';
import { useLang } from '../i18n';
import { playClick, tap } from '../sound/click';

interface Props {
  onStart: (mode: SoloMode, level: number) => void;
  onBack: () => void;
}

export default function SoloSetupScreen({ onStart, onBack }: Props) {
  const [mode, setMode] = useState<SoloMode>('endless');
  const [level, setLevel] = useState(1);
  const { t } = useLang();

  const cfg = LEVELS[level - 1] ?? LEVELS[0]!;

  return (
    <div className="pd-frame">
      <p className="pd-status">{t('soloIntro')}</p>

      <div className="pd-panel">
        <div className="pd-row">
          <button
            className={`pd-btn pd-btn--outline${mode === 'endless' ? ' pd-btn--active' : ''}`}
            onClick={() => {
              playClick();
              setMode('endless');
            }}
          >
            {t('training')}
          </button>
          <button
            className={`pd-btn pd-btn--outline${mode === 'match' ? ' pd-btn--active' : ''}`}
            onClick={() => {
              playClick();
              setMode('match');
            }}
          >
            {t('matchMode')}
          </button>
        </div>
        <p className="pd-status">
          {mode === 'endless' ? t('endlessDesc') : t('matchDesc')}
        </p>
      </div>

      <div className="pd-panel">
        <p className="pd-status">{t('level')}</p>
        <div className="pd-level-grid">
          {LEVELS.map((l) => (
            <button
              key={l.level}
              className={`pd-key${l.level === level ? ' pd-key--active' : ''}`}
              onClick={() => {
                playClick();
                setLevel(l.level);
              }}
            >
              {l.level}
            </button>
          ))}
        </div>
        <p className="pd-status">
          {t('levelInfo', { min: cfg.minDigits, max: cfg.maxDigits, tmin: cfg.targetMin, tmax: cfg.targetMax, sec: cfg.timeLimitSec })}
        </p>
      </div>

      <button
        className="pd-btn pd-btn--primary"
        onClick={() => {
          playClick();
          onStart(mode, level);
        }}
      >
        {t('start')}
      </button>
      <button className="pd-btn pd-btn--outline" onClick={tap(onBack)}>
        {t('back')}
      </button>
    </div>
  );
}
