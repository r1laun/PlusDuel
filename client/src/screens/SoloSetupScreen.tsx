import { useState } from 'react';
import { LEVELS } from '@plusduel/shared';
import type { SoloMode } from '../solo/engine';
import { getSoloLifetimeStats } from '../hooks/useSoloGame';
import { PlusDuelLogo } from '../components/ThemeButton';
import { useLang } from '../i18n';
import { playClick, tap } from '../sound/click';

interface Props {
  onStart: (mode: SoloMode, level: number) => void;
  onBack: () => void;
}

export default function SoloSetupScreen({ onStart, onBack }: Props) {
  const [mode, setMode] = useState<SoloMode>('endless');
  const [level, setLevel] = useState(2);
  const { t } = useLang();
  const lifetime = getSoloLifetimeStats();

  const cfg = LEVELS[level - 1] ?? LEVELS[0]!;

  return (
    <div className="pd-frame">
      <PlusDuelLogo compact />
      <p className="pd-tagline">{t('soloIntro')}</p>

      {/* PlusMath PracticeScreen Lifetime Stats Card */}
      <div className="pd-panel">
        <div className="pd-stats-grid">
          <div>
            <div className="pd-stat__val">{lifetime.totalSolved}</div>
            <div className="pd-stat__label">{t('soloSolvedTotal')}</div>
          </div>
          <div>
            <div className="pd-stat__val pd-stat__val--ink">{lifetime.bestStreak}</div>
            <div className="pd-stat__label">{t('soloBestStreak')}</div>
          </div>
        </div>
      </div>

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
        <p className="pd-status pd-status--muted">
          {mode === 'endless' ? t('endlessDesc') : t('matchDesc')}
        </p>
      </div>

      <div className="pd-panel">
        <p className="pd-status">{t('level')} (1–6)</p>
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
        <div className="pd-level-spec">
          {t('levelInfo', {
            min: cfg.minDigits,
            max: cfg.maxDigits,
            tmin: cfg.targetMin,
            tmax: cfg.targetMax,
            sec: cfg.timeLimitSec,
          })}
        </div>
      </div>

      <button
        className="pd-btn pd-btn--accent"
        onClick={() => {
          playClick();
          onStart(mode, level);
        }}
      >
        {t('start')} ▶
      </button>
      <button className="pd-btn pd-btn--outline" onClick={tap(onBack)}>
        ← {t('back')}
      </button>
    </div>
  );
}
