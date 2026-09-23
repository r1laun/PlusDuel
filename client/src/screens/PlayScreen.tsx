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

  return (
    <div className="pd-frame">
      <div className="pd-topbar">
        <div
          className="pd-badge"
          role="button"
          tabIndex={0}
          title="Leave match"
          onClick={onLeave}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') onLeave();
          }}
        >
          EXT
        </div>
        {active ? (
          <TimerBar timeLimitMs={round.timeLimitMs} receivedAt={roundReceivedAt} />
        ) : (
          <div className="pd-display pd-display--accent">0.0s</div>
        )}
        <div className="pd-badge">R{round.index}</div>
      </div>

      <div className="pd-display">{round.target}</div>

      {error && <div className="pd-status">{error}</div>}

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
        <div className="pd-panel">
          {roundEnd!.winnerId === null ? (
            <>
              <div className="pd-status">Timeout</div>
              {roundEnd!.sampleSolution && (
                <div className="pd-info-box pd-info-box--accent">
                  {pretty(roundEnd!.sampleSolution)} = {round.target}
                </div>
              )}
            </>
          ) : (
            <>
              <div className={`pd-status${iWon ? ' pd-status--accent' : ''}`}>
                {iWon ? 'You won' : `${matchInfo.opponent.name} won`}
              </div>
              <div className="pd-info-box pd-info-box--accent">
                {pretty(roundEnd!.winningExpression ?? '')} = {round.target}
              </div>
            </>
          )}
          <div className="pd-status">Next round…</div>
        </div>
      )}
    </div>
  );
}

function pretty(expr: string): string {
  return expr.replace(/\*/g, '×').replace(/\//g, '÷').replace(/-/g, '−');
}
