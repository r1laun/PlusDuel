import { useState } from 'react';
import type { RankingsPayload } from '@plusduel/shared';
import type { Session } from '@supabase/supabase-js';
import { playClick, tap } from '../sound/click';
import { useLang } from '../i18n';
import AuthButton from '../components/AuthButton';

interface Props {
  name: string;
  setName: (n: string) => void;
  queued: boolean;
  queuePos: number;
  error: string;
  roomCode: string;
  serverUp: boolean;
  rankings: RankingsPayload | null;
  session: Session | null;
  onQuickPlay: () => void;
  onCreatePrivate: () => void;
  onJoinPrivate: (code: string) => void;
  onCancelQueue: () => void;
  onSolo: () => void;
}

export default function HomeScreen({
  name,
  setName,
  queued,
  queuePos,
  error,
  roomCode,
  serverUp,
  rankings,
  session,
  onQuickPlay,
  onCreatePrivate,
  onJoinPrivate,
  onCancelQueue,
  onSolo,
}: Props) {
  const [code, setCode] = useState('');
  const [showRules, setShowRules] = useState(false);
  const { t, titleLabel, label: langLabel, cycle: cycleLang } = useLang();

  return (
    <div className="pd-frame">
      <div className="pd-header">
        <button
          className="pd-btn pd-btn--outline pd-btn--sm"
          onClick={cycleLang}
          title="Language"
          aria-label="Language"
        >
          {langLabel}
        </button>
        <AuthButton session={session} compact />
      </div>
      <div className="pd-logo">
        <span className="pd-logo__plus">Plus</span>
        <span className="pd-logo__dual">Duel</span>
      </div>
      <p className="pd-tagline">{t('tagline')}</p>

      {error && <div className="pd-status">{error}</div>}
      {!serverUp && !queued && (
        <div className="pd-status">{t('serverDown')}</div>
      )}

      {queued ? (
        <div className="pd-panel">
          {queuePos === -1 ? (
            <>
              <p className="pd-status">{t('roomOpen')}</p>
              {roomCode && (
                <button
                  className="pd-room-code"
                  onClick={() => {
                    playClick();
                    navigator.clipboard?.writeText(roomCode).catch(() => {});
                  }}
                  title={t('clickToCopy')}
                >
                  <span className="pd-room-code__label">{t('yourCode')}</span>
                  <span className="pd-room-code__value">{roomCode}</span>
                  <span className="pd-room-code__hint">{t('tapToCopy')}</span>
                </button>
              )}
            </>
          ) : (
            <p className="pd-status">{t('looking')}</p>
          )}
          <button className="pd-btn pd-btn--primary" onClick={tap(onCancelQueue)}>
            {t('cancel')}
          </button>
        </div>
      ) : (
        <>
          <div className="pd-panel">
            <div className="pd-field">
              <label className="pd-label" htmlFor="pd-name">
                {t('nickname')}
              </label>
              <input
                id="pd-name"
                className="pd-input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t('anonymous')}
                maxLength={20}
                autoComplete="off"
              />
            </div>

            <div className="pd-row">
              <button className="pd-btn pd-btn--primary" onClick={tap(onQuickPlay)}>
                {t('quickPlay')}
              </button>
              <button className="pd-btn pd-btn--outline" onClick={tap(onSolo)}>
                {t('practiceSolo')}
              </button>
            </div>
          </div>

          <hr className="pd-divider" />

          <div className="pd-panel">
            <button className="pd-btn pd-btn--outline" onClick={tap(onCreatePrivate)}>
              {t('createPrivate')}
            </button>

            <div className="pd-field">
              <input
                className="pd-input"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                placeholder={t('codePh')}
                maxLength={6}
                autoComplete="off"
                aria-label={t('codePh')}
              />
              <button
                className="pd-btn pd-btn--outline pd-btn--sm"
                disabled={code.trim().length < 4}
                onClick={() => {
                  playClick();
                  onJoinPrivate(code);
                }}
              >
                {t('join')}
              </button>
            </div>
          </div>

          <hr className="pd-divider" />

          <div className="pd-panel">
            <p className="pd-status">{t('leaderboard')}</p>
            {rankings?.you && (
              <p className="pd-status pd-status--you">
                {t('youLine', { rating: rankings.you.rating, title: titleLabel(rankings.you.title) })}
              </p>
            )}
            {rankings && rankings.top.length > 0 ? (
              rankings.top.map((e, i) => (
                <div
                  key={e.playerId}
                  className={`pd-scoreboard__row${rankings.you && e.playerId === rankings.you.playerId ? ' pd-scoreboard__row--you' : ''}`}
                >
                  <span>
                    #{i + 1} {e.name}
                  </span>
                  <div className="pd-scoreboard__bar" />
                  <span className="pd-scoreboard__score">
                    {e.rating} · {titleLabel(e.title)}
                  </span>
                </div>
              ))
            ) : (
              <p className="pd-status">{t('noRanked')}</p>
            )}
          </div>

          <hr className="pd-divider" />

          <button
            className="pd-btn pd-btn--outline"
            onClick={() => {
              playClick();
              setShowRules((s) => !s);
            }}
          >
            {showRules ? t('hideRules') : t('rules')}
          </button>

          {showRules && (
            <div className="pd-panel">
              <p className="pd-status">{t('rule1')}</p>
              <p className="pd-status">{t('rule2')}</p>
              <p className="pd-status">{t('rule3')}</p>
              <p className="pd-status">{t('rule4')}</p>
              <p className="pd-status">{t('rule5')}</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
