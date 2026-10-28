import { useCallback, useEffect, useMemo, useState } from 'react';
import type {
  MatchStartPayload,
  RoundEndPayload,
  RoundStartPayload,
} from '@plusduel/shared';
import ExpressionInput from '../components/ExpressionInput';
import TimerBar from '../components/TimerBar';
import { useLocalValidation } from '../hooks/useLocalValidation';
import { useLang } from '../i18n';
import { playClick, playRoundLose, playRoundWin } from '../sound/click';

interface Props {
  myId: string;
  matchInfo: MatchStartPayload;
  round: RoundStartPayload;
  roundReceivedAt: number;
  roundEnd: RoundEndPayload | null;
  error: string;
  onSubmit: (expression: string) => void;
  onLeave: () => void;
  soloStats?: { solved: number; failed: number; score: number };
}

const OP_SYMBOLS = ['+', '−', '×', '÷', '(', ')', '^', '√', '!'];

export default function PlayScreen({
  myId,
  matchInfo,
  round,
  roundReceivedAt,
  roundEnd,
  error,
  onSubmit,
  onLeave,
  soloStats,
}: Props) {
  const [expr, setExpr] = useState('');
  const [wins, setWins] = useState<{ me: number; opp: number }>({ me: 0, opp: 0 });
  const { t } = useLang();
  const isTouch = useMemo(
    () => typeof window !== 'undefined' && window.matchMedia('(pointer: coarse)').matches,
    [],
  );

  useEffect(() => {
    setExpr('');
  }, [round.index]);

  useEffect(() => {
    if (!roundEnd) return;
    const me = roundEnd.winsByPlayer[myId] ?? 0;
    const opp = roundEnd.winsByPlayer[matchInfo.opponent.id] ?? 0;
    setWins({ me, opp });
    if (roundEnd.winnerId === myId) playRoundWin();
    else playRoundLose();
  }, [roundEnd, myId, matchInfo.opponent.id]);

  const validation = useLocalValidation(expr, round.digits, round.target, roundEnd === null);
  const active = roundEnd === null;

  const submit = useCallback(() => {
    if (!active || !validation.valid) return;
    onSubmit(expr);
  }, [active, validation.valid, expr, onSubmit]);

  const iWon = roundEnd?.winnerId === myId;

  const leave = () => {
    playClick();
    onLeave();
  };

  const usedCount = Math.min(
    round.digits.length,
    round.digits.length - validation.missingDigits.length,
  );

  return (
    <div className="pd-frame">
      <div className="pd-topbar">
        <div
          className="pd-badge"
          role="button"
          tabIndex={0}
          title={t('leaveTitle')}
          onClick={leave}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') leave();
          }}
        >
          ← EXT
        </div>
        {active ? (
          <TimerBar timeLimitMs={round.timeLimitMs} receivedAt={roundReceivedAt} />
        ) : (
          <div className="pd-timer">
            <span className="pd-timer__text">0.0s</span>
          </div>
        )}
        <div className="pd-badge pd-badge--score">
          {wins.me} : {wins.opp}
        </div>
        <div className="pd-badge">R{round.index}</div>
      </div>

      {/* PlusMath-style Target Card */}
      <div className="pd-target-card">
        <div className="pd-target-card__top">
          <span>{t('targetHeader')}</span>
          <span className="pd-target-card__meta">
            {matchInfo.opponent.name}
          </span>
        </div>
        <div className="pd-target-card__value">{round.target}</div>
        <div className="pd-target-card__sub">
          {t('useEveryDigitOnce', { used: usedCount, total: round.digits.length })}
        </div>
      </div>

      {matchInfo.rated === false && <div className="pd-status pd-status--muted">{t('unrated')}</div>}

      {error && <div className="pd-status-banner pd-status-banner--error">{error}</div>}

      {active ? (
        <ExpressionInput
          expr={expr}
          setExpr={setExpr}
          digits={round.digits}
          symbols={OP_SYMBOLS}
          isTouch={isTouch}
          disabled={!active}
          validation={validation}
          onSubmit={submit}
        />
      ) : (
        <div className="pd-panel pd-panel--accent">
          {roundEnd!.winnerId === null ? (
            <>
              <div className="pd-status">{t('timeout')}</div>
              {roundEnd!.sampleSolution && (
                <div className="pd-info-box pd-info-box--accent">
                  {pretty(roundEnd!.sampleSolution)} = {round.target}
                </div>
              )}
            </>
          ) : (
            <>
              <div className={`pd-status${iWon ? ' pd-status--accent' : ''}`}>
                {iWon ? t('youWon') : t('oppWon', { name: matchInfo.opponent.name })}
              </div>
              <div className="pd-info-box pd-info-box--accent">
                {pretty(roundEnd!.winningExpression ?? '')} = {round.target}
              </div>
            </>
          )}
          <div className="pd-status pd-status--muted">{t('nextRound')}</div>
          {soloStats && (
            <div className="pd-status">
              {t('soloLine', {
                solved: soloStats.solved,
                failed: soloStats.failed,
                score: soloStats.score.toFixed(1),
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function pretty(expr: string): string {
  return expr.replace(/\*/g, '×').replace(/\//g, '÷').replace(/-/g, '−');
}
