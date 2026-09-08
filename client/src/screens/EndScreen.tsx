import { useEffect, useState } from 'react';
import type { MatchEndPayload, MatchStartPayload } from '@plusduel/shared';

interface Props {
  myId: string;
  matchEnd: MatchEndPayload;
  matchInfo: MatchStartPayload | null;
  onHome: () => void;
}

export default function EndScreen({ myId, matchEnd, matchInfo, onHome }: Props) {
  const [countdown, setCountdown] = useState(10);
  const won = matchEnd.winnerId === myId;

  useEffect(() => {
    const t = setInterval(() => setCountdown((c) => Math.max(0, c - 1)), 1000);
    return () => clearInterval(t);
  }, []);

  useEffect(() => {
    if (countdown === 0) onHome();
  }, [countdown, onHome]);

  const opponentName = matchInfo?.opponent.name ?? 'Opponent';
  const myScore = matchEnd.scoresByPlayer[myId] ?? 0;
  const oppId = matchInfo?.opponent.id;
  const oppScore = oppId ? (matchEnd.scoresByPlayer[oppId] ?? 0) : 0;

  return (
    <div className="screen end">
      <h1 className={won ? 'win' : 'lose'}>{won ? 'Victory' : 'Defeat'}</h1>
      {matchEnd.reason === 'forfeit' && <p className="muted">Match ended by forfeit.</p>}
      <div className="card final-stats">
        <div className="stat-row">
          <span>You</span>
          <span className="score">{myScore.toFixed(1)}</span>
        </div>
        <div className="stat-row">
          <span>{opponentName}</span>
          <span className="score">{oppScore.toFixed(1)}</span>
        </div>
      </div>
      <button className="btn primary big" onClick={onHome}>
        Back to lobby{countdown > 0 ? ` (${countdown})` : ''}
      </button>
    </div>
  );
}
