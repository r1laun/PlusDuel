import { useCallback, useEffect, useMemo, useState } from 'react';
import type {
  MatchStartPayload,
  RoundEndPayload,
  RoundStartPayload,
} from '@plusduel/shared';
import ExpressionInput from '../components/ExpressionInput';
import TimerBar from '../components/TimerBar';
import { useLocalValidation } from '../hooks/useLocalValidation';

interface Props {
  myId: string;
  matchInfo: MatchStartPayload;
  round: RoundStartPayload;
  roundReceivedAt: number;
  roundEnd: RoundEndPayload | null;
  error: string;
  onSubmit: (expression: string) => void;
  onLeave: () => void;
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
}: Props) {
  const [expr, setExpr] = useState('');
  const isTouch = useMemo(
    () => typeof window !== 'undefined' && window.matchMedia('(pointer: coarse)').matches,
    [],
  );

  useEffect(() => {
    setExpr('');
  }, [round.index]);

  const validation = useLocalValidation(expr, round.digits, round.target, roundEnd === null);
  const active = roundEnd === null;

  const submit = useCallback(() => {
    if (!active || !validation.valid) return;
    onSubmit(expr);
  }, [active, validation.valid, expr, onSubmit]);

  const iWon = roundEnd?.winnerId === myId;
  const opponentSolved = roundEnd !== null && roundEnd.winnerId !== null && roundEnd.winnerId !== myId;

  return (
    <div className="screen play">
      <header className="match-bar">
        <button className="icon-btn" onClick={onLeave} title="Leave match">
          ✕
        </button>
        <div className="players">
          <span className="player-name">
            <span className="dot live" /> You
          </span>
          <span className="vs">vs</span>
          <span className="player-name">
            <span className={`dot ${opponentSolved ? 'solved' : 'live'}`} />
            {matchInfo.opponent.name}
          </span>
        </div>
        <span className="round-badge">R{round.index}</span>
      </header>

      <section className="target-section">
        <div className="target-number">{round.target}</div>
        <div className="digits-row">
          {round.digits.map((d, i) => (
            <span key={`${round.index}-${i}`} className="tile-display">{d}</span>
          ))}
        </div>
        {active && (
          <TimerBar timeLimitMs={round.timeLimitMs} receivedAt={roundReceivedAt} />
        )}
      </section>

      {error && <div className="error-msg">{error}</div>}

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
        <div className="round-result">
          {roundEnd!.winnerId === null ? (
            <>
              <h2>Timeout</h2>
              {roundEnd!.sampleSolution && (
                <p className="solution-hint">
                  {pretty(roundEnd!.sampleSolution)} = {round.target}
                </p>
              )}
            </>
          ) : (
            <>
              <h2 className={iWon ? 'win' : 'lose'}>
                {iWon ? 'You won' : `${matchInfo.opponent.name} won`}
              </h2>
              <p className="solution-hint">
                {pretty(roundEnd!.winningExpression ?? '')} = {round.target}
              </p>
            </>
          )}
          <p className="next-round">Next round…</p>
        </div>
      )}

      <footer className="match-footer">
        <span>
          {matchInfo.roundsToWin === 3 ? 'Best of 5' : `Best of ${matchInfo.roundsToWin * 2 - 1}`}
        </span>
      </footer>
    </div>
  );
}

function pretty(expr: string): string {
  return expr.replace(/\*/g, '×').replace(/\//g, '÷').replace(/-/g, '−');
}
