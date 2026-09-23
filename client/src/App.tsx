import { useCallback, useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import type {
  ErrorPayload,
  MatchEndPayload,
  MatchStartPayload,
  RoundEndPayload,
  RoundStartPayload,
} from '@plusduel/shared';
import { socket } from './socket';
import HomeScreen from './screens/HomeScreen';
import PlayScreen from './screens/PlayScreen';
import EndScreen from './screens/EndScreen';

type Phase = 'home' | 'queued' | 'playing' | 'ended';

export default function App() {
  const [phase, setPhase] = useState<Phase>('home');
  const [name, setName] = useState(() => localStorage.getItem('pd:name') ?? '');
  const [queuePos, setQueuePos] = useState(0);
  const [matchInfo, setMatchInfo] = useState<MatchStartPayload | null>(null);
  const [round, setRound] = useState<RoundStartPayload | null>(null);
  const [roundReceivedAt, setRoundReceivedAt] = useState(0);
  const [roundEnd, setRoundEnd] = useState<RoundEndPayload | null>(null);
  const [matchEnd, setMatchEnd] = useState<MatchEndPayload | null>(null);
  const [error, setError] = useState('');
  const myId = useRef<string>(socket.id ?? '');

  useEffect(() => {
    socket.connect();

    const onMatchStart = (p: MatchStartPayload) => {
      myId.current = p.youAre;
      setMatchInfo(p);
      setRoundEnd(null);
      setMatchEnd(null);
      setPhase('playing');
    };
    const onRoundStart = (p: RoundStartPayload) => {
      setRound(p);
      setRoundEnd(null);
      setRoundReceivedAt(Date.now());
      setError('');
    };
    const onRoundEnd = (p: RoundEndPayload) => setRoundEnd(p);
    const onMatchEnd = (p: MatchEndPayload) => {
      setMatchEnd(p);
      setPhase('ended');
    };
    const onError = (p: ErrorPayload) => setError(p.message);
    const onQueued = (p: { position: number }) => setQueuePos(p.position);
    const onOpponentLeft = () => setError('Opponent left the match.');

    socket.on('game:queued', onQueued);
    socket.on('match:start', onMatchStart);
    socket.on('round:start', onRoundStart);
    socket.on('round:end', onRoundEnd);
    socket.on('match:end', onMatchEnd);
    socket.on('game:error', onError);
    socket.on('opponent:left', onOpponentLeft);

    return () => {
      socket.off('game:queued', onQueued);
      socket.off('match:start', onMatchStart);
      socket.off('round:start', onRoundStart);
      socket.off('round:end', onRoundEnd);
      socket.off('match:end', onMatchEnd);
      socket.off('game:error', onError);
      socket.off('opponent:left', onOpponentLeft);
    };
  }, []);

  useEffect(() => {
    localStorage.setItem('pd:name', name);
  }, [name]);

  const quickPlay = useCallback(() => {
    setError('');
    socket.emit('game:queue_join', { name });
    setPhase('queued');
  }, [name]);

  const createPrivate = useCallback(() => {
    setError('');
    socket.emit('game:create_private', { name }, (res) => {
      setQueuePos(-1);
      setPhase('queued');
      if (res?.code) {
        navigator.clipboard?.writeText(res.code).catch(() => {});
        setError(`Room code ${res.code} copied to clipboard — share it!`);
      }
    });
  }, [name]);

  const joinPrivate = useCallback(
    (code: string) => {
      setError('');
      // Optimistic: show the waiting screen immediately. Do NOT set the phase
      // from the ack — the server sends match:start after join_private, and
      // the ack arrives AFTER it, so re-setting 'queued' here would strand
      // the guest on the waiting screen while the host is already playing.
      setPhase('queued');
      socket.emit('game:join_private', { name, code }, (res) => {
        if (!res?.joined) {
          setPhase('home');
          setError(`Could not join room "${code}".`);
        }
      });
    },
    [name],
  );

  const submitExpression = useCallback((expression: string) => {
    socket.emit('round:submit', { expression }, () => {});
  }, []);

  const leaveMatch = useCallback(() => {
    socket.emit('game:queue_leave');
    socket.disconnect();
    socket.connect();
    setPhase('home');
    setRound(null);
    setRoundEnd(null);
    setMatchEnd(null);
    setMatchInfo(null);
    setError('');
  }, []);

  const backHome = useCallback(() => {
    setPhase('home');
    setRound(null);
    setRoundEnd(null);
    setMatchEnd(null);
    setMatchInfo(null);
    setError('');
  }, []);

  let content: ReactNode;
  if (phase === 'playing' && matchInfo && round) {
    content = (
      <PlayScreen
        key={matchInfo.roomCode ?? 'duel'}
        myId={myId.current}
        matchInfo={matchInfo}
        round={round}
        roundReceivedAt={roundReceivedAt}
        roundEnd={roundEnd}
        error={error}
        onSubmit={submitExpression}
        onLeave={leaveMatch}
      />
    );
  } else if (phase === 'ended' && matchEnd) {
    content = (
      <EndScreen
        myId={myId.current}
        matchEnd={matchEnd}
        matchInfo={matchInfo}
        onHome={backHome}
      />
    );
  } else {
    content = (
      <HomeScreen
        name={name}
        setName={setName}
        queued={phase === 'queued'}
        queuePos={queuePos}
        error={error}
        onQuickPlay={quickPlay}
        onCreatePrivate={createPrivate}
        onJoinPrivate={joinPrivate}
        onCancelQueue={() => {
          socket.emit('game:queue_leave');
          setPhase('home');
        }}
      />
    );
  }

  return <div className="pd-app">{content}</div>;
}
