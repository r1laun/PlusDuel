import { useCallback, useEffect, useRef, useState, lazy, Suspense } from 'react';
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
import { useSoloGame } from './hooks/useSoloGame';
import { SOLO_YOU } from './solo/engine';
import { playClick } from './sound/click';
// Split the duel UI (and its validation chain) out of the initial bundle —
// it loads on demand when a match starts, keeping first paint light.
const PlayScreen = lazy(() => import('./screens/PlayScreen'));
const SoloSetupScreen = lazy(() => import('./screens/SoloSetupScreen'));
import EndScreen from './screens/EndScreen';

type Phase = 'home' | 'queued' | 'playing' | 'ended' | 'solo-setup' | 'solo';

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
  const [roomCode, setRoomCode] = useState('');
  const myId = useRef<string>(socket.id ?? '');
  const solo = useSoloGame();

  useEffect(() => {
    socket.connect();

    const onMatchStart = (p: MatchStartPayload) => {
      myId.current = p.youAre;
      setMatchInfo(p);
      setRoundEnd(null);
      setMatchEnd(null);
      setRoomCode('');
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
        setRoomCode(res.code);
        navigator.clipboard?.writeText(res.code).catch(() => {});
      }
    });
  }, [name]);

  const joinPrivate = useCallback(
    (code: string) => {
      setError('');
      setRoomCode('');
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
    setRoomCode('');
    setError('');
  }, []);

  const backHome = useCallback(() => {
    setPhase('home');
    setRound(null);
    setRoundEnd(null);
    setMatchEnd(null);
    setMatchInfo(null);
    setRoomCode('');
    setError('');
  }, []);

  const leaveSolo = useCallback(() => {
    solo.leave();
    setPhase('home');
  }, [solo.leave]);

  let content: ReactNode;
  if (phase === 'solo-setup') {
    content = (
      <Suspense
        fallback={
          <div className="pd-frame">
            <p className="pd-status">Loading practice…</p>
          </div>
        }
      >
        <SoloSetupScreen
          onStart={(mode, level) => {
            solo.start(mode, level);
            setPhase('solo');
          }}
          onBack={() => setPhase('home')}
        />
      </Suspense>
    );
  } else if (phase === 'solo' && solo.matchEnd && solo.matchInfo) {
    content = (
      <EndScreen myId={SOLO_YOU} matchEnd={solo.matchEnd} matchInfo={solo.matchInfo} onHome={leaveSolo} />
    );
  } else if (phase === 'solo' && solo.matchInfo && solo.round) {
    content = (
      <Suspense
        fallback={
          <div className="pd-frame">
            <p className="pd-status">Loading practice…</p>
          </div>
        }
      >
        <PlayScreen
          key="solo"
          myId={SOLO_YOU}
          matchInfo={solo.matchInfo}
          round={solo.round}
          roundReceivedAt={solo.roundReceivedAt}
          roundEnd={solo.roundEnd}
          error={solo.error}
          onSubmit={solo.submit}
          onLeave={leaveSolo}
          soloStats={solo.stats}
        />
      </Suspense>
    );
  } else if (phase === 'solo') {
    content = (
      <div className="pd-frame">
        <p className="pd-status">Generating round…</p>
        <button
          className="pd-btn pd-btn--outline"
          onClick={() => {
            playClick();
            leaveSolo();
          }}
        >
          Back
        </button>
      </div>
    );
  } else if (phase === 'playing' && matchInfo && round) {
    content = (
      <Suspense
        fallback={
          <div className="pd-frame">
            <p className="pd-status">Loading duel…</p>
          </div>
        }
      >
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
      </Suspense>
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
        roomCode={roomCode}
        onQuickPlay={quickPlay}
        onCreatePrivate={createPrivate}
        onJoinPrivate={joinPrivate}
        onCancelQueue={() => {
          socket.emit('game:queue_leave');
          setRoomCode('');
          setPhase('home');
        }}
        onSolo={() => setPhase('solo-setup')}
      />
    );
  }

  return <main className="pd-app">{content}</main>;
}
