import { useEffect, useState } from 'react';
import type { MatchEndPayload, MatchStartPayload } from '@plusduel/shared';
import { tap, playMatchDraw, playMatchLose, playMatchWin } from '../sound/click';

interface Props {
  myId: string;
  matchEnd: MatchEndPayload;
  matchInfo: MatchStartPayload | null;
  onHome: () => void;
}

export default function EndScreen({ myId, matchEnd, matchInfo, onHome }: Props) {
  const [countdown, setCountdown] = useState(10);
  const draw = matchEnd.winnerId === null && matchEnd.reason !== 'forfeit';
  const won = !draw && matchEnd.winnerId === myId;

  useEffect(() => {
    const t = setInterval(() => setCountdown((c) => Math.max(0, c - 1)), 1000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    if (draw) playMatchDraw();
    else if (won) playMatchWin();
    else playMatchLose();
  }, []);

  useEffect(() => {
    if (countdown === 0) onHome();
  }, [countdown, onHome]);

  const opponentName = matchInfo?.opponent.name ?? 'Opponent';
  const myScore = matchEnd.scoresByPlayer[myId] ?? 0;
  const oppId = matchInfo?.opponent.id;
  const oppScore = oppId ? (matchEnd.scoresByPlayer[oppId] ?? 0) : 0;
  const rating = matchEnd.ratingsByPlayer?.[myId];
  const delta = rating ? rating.after - rating.before : null;

  return (
    <div className="pd-frame">
      <h1 className="pd-title--victory">{draw ? 'Draw' : won ? 'Victory' : 'Defeat'}</h1>
      <div className="pd-status">
        {matchEnd.reason === 'forfeit'
          ? 'Match ended by forfeit.'
          : draw
            ? 'Nobody reached 3 wins in 5 rounds — draw.'
            : won
              ? 'You won the match.'
              : `${opponentName} won the match.`}
      </div>
      <div className="pd-panel">
        {rating && delta !== null && (
          <div className="pd-info-box pd-info-box--accent">
            Elo {rating.after} ({delta >= 0 ? `+${delta}` : delta}) · {rating.title}
          </div>
        )}
        <div className="pd-scoreboard__row">
          <span>You</span>
          <div className="pd-scoreboard__bar" />
          <span className="pd-scoreboard__score">{myScore.toFixed(1)}</span>
        </div>
        <div className="pd-scoreboard__row">
          <span>{opponentName}</span>
          <div className="pd-scoreboard__bar" />
          <span className="pd-scoreboard__score">{oppScore.toFixed(1)}</span>
        </div>
      </div>
      <button className="pd-btn pd-btn--primary" onClick={tap(onHome)}>
        Back to lobby{countdown > 0 ? ` (${countdown})` : ''}
      </button>
    </div>
  );
}
