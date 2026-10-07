import { useCallback, useEffect, useRef, useState, lazy, Suspense } from 'react';
import type { ReactNode } from 'react';
import type {
  ErrorPayload,
  MatchEndPayload,
  MatchStartPayload,
  RankingsPayload,
  RoundEndPayload,
  RoundStartPayload,
} from '@plusduel/shared';
import { socket, setSocketToken } from './socket';
import HomeScreen from './screens/HomeScreen';
import { useSoloGame } from './hooks/useSoloGame';
import { SOLO_YOU } from './solo/engine';
import { playClick } from './sound/click';
import { safeGet, safeSet, getPlayerId } from './storage';
import { accessToken, supabase } from './auth/supabase';
import { useLang } from './i18n';
import type { Session } from '@supabase/supabase-js';
// Split the duel UI (and its validation chain) out of the initial bundle -
// it loads on demand when a match starts, keeping first paint light.
const PlayScreen = lazy(() => import('./screens/PlayScreen'));
const SoloSetupScreen = lazy(() => import('./screens/SoloSetupScreen'));
import EndScreen from './screens/EndScreen';

type Phase = 'home' | 'queued' | 'playing' | 'ended' | 'solo-setup' | 'solo';

export default function App() {
  const [phase, setPhase] = useState<Phase>('home');
  const [name, setName] = useState(() => safeGet('pd:name'));
  const [queuePos, setQueuePos] = useState(0);
  const [matchInfo, setMatchInfo] = useState<MatchStartPayload | null>(null);
  const [round, setRound] = useState<RoundStartPayload | null>(null);
  const [roundReceivedAt, setRoundReceivedAt] = useState(0);
  const [roundEnd, setRoundEnd] = useState<RoundEndPayload | null>(null);
  const [matchEnd, setMatchEnd] = useState<MatchEndPayload | null>(null);
  const [error, setError] = useState('');
  const [roomCode, setRoomCode] = useState('');
  // Optimistic: assume the server is up until a connect error proves otherwise.
  const [serverUp, setServerUp] = useState(true);
  const [rankings, setRankings] = useState<RankingsPayload | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const myId = useRef<string>(socket.id ?? '');
  const solo = useSoloGame();
  const { t } = useLang();
  // Handlers registered once (socket effect) always use the current language.
  const tRef = useRef(t);
  tRef.current = t;

  const refreshRankings = useCallback(() => {
    socket.emit('rankings:get', (res) => {
      if (res) setRankings(res);
    });
  }, []);

  const connectSocket = useCallback(async () => {
    setSocketToken(await accessToken());
    if (!socket.connected) socket.connect();
  }, []);

  // Supabase session (null = guest). No-ops entirely when auth isn't configured.
  useEffect(() => {
    const sb = supabase();
    if (!sb) return;
    sb.auth.getSession().then(({ data }) => setSession(data.session));
    const { data: sub } = sb.auth.onAuthStateChange((_event, s) => setSession(s));
    return () => {
      sub.subscription.unsubscribe();
    };
  }, []);

  // Reconnect with the right identity on sign-in/out transitions only.
  const sessionRef = useRef<Session | null>(null);
  useEffect(() => {
    const prev = sessionRef.current;
    sessionRef.current = session;
    if (session && !prev) {
      // Signed in: reconnect authed, then migrate guest Elo once.
      void (async () => {
        setSocketToken(session.access_token);
        socket.disconnect();
        socket.connect();
        socket.emit('account:link', { devicePlayerId: getPlayerId() }, () => refreshRankings());
      })();
    } else if (!session && prev) {
      // Signed out: drop the authed connection, continue as guest.
      setSocketToken(null);
      socket.disconnect();
      socket.connect();
      refreshRankings();
    }
  }, [session, refreshRankings]);

  useEffect(() => {
    void connectSocket();
    refreshRankings();

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
    const onOpponentLeft = () => setError(tRef.current('opponentLeft'));
    const onConnect = () => setServerUp(true);
    const onConnectError = () => setServerUp(false);
    const onDisconnect = () => setServerUp(false);

    socket.on('game:queued', onQueued);
    socket.on('match:start', onMatchStart);
    socket.on('round:start', onRoundStart);
    socket.on('round:end', onRoundEnd);
    socket.on('match:end', onMatchEnd);
    socket.on('game:error', onError);
    socket.on('opponent:left', onOpponentLeft);
    socket.on('connect', onConnect);
    socket.on('connect_error', onConnectError);
    socket.on('disconnect', onDisconnect);

    return () => {
      socket.off('game:queued', onQueued);
      socket.off('match:start', onMatchStart);
      socket.off('round:start', onRoundStart);
      socket.off('round:end', onRoundEnd);
      socket.off('match:end', onMatchEnd);
      socket.off('game:error', onError);
      socket.off('opponent:left', onOpponentLeft);
      socket.off('connect', onConnect);
      socket.off('connect_error', onConnectError);
      socket.off('disconnect', onDisconnect);
    };
  }, []);

  useEffect(() => {
    safeSet('pd:name', name);
  }, [name]);

  const quickPlay = useCallback(() => {
    setError('');
    socket.emit('game:queue_join', { name, playerId: getPlayerId() });
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
      // from the ack - the server sends match:start after join_private, and
      // the ack arrives AFTER it, so re-setting 'queued' here would strand
      // the guest on the waiting screen while the host is already playing.
      setPhase('queued');
      socket.emit('game:join_private', { name, code }, (res) => {
        if (!res?.joined) {
          setPhase('home');
          setError(tRef.current('joinFail', { code }));
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
    void connectSocket();
    setPhase('home');
    setRound(null);
    setRoundEnd(null);
    setMatchEnd(null);
    setMatchInfo(null);
    setRoomCode('');
    setError('');
    refreshRankings();
  }, [refreshRankings]);

  const backHome = useCallback(() => {
    setPhase('home');
    setRound(null);
    setRoundEnd(null);
    setMatchEnd(null);
    setMatchInfo(null);
    setRoomCode('');
    setError('');
    refreshRankings();
  }, [refreshRankings]);

  const leaveSolo = useCallback(() => {
    solo.leave();
    setPhase('home');
    refreshRankings();
  }, [solo.leave, refreshRankings]);

  let content: ReactNode;
  if (phase === 'solo-setup') {
    content = (
      <Suspense
        fallback={
          <div className="pd-frame">
            <p className="pd-status">{t('loadingPractice')}</p>
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
            <p className="pd-status">{t('loadingPractice')}</p>
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
        <p className="pd-status">{t('generating')}</p>
        <button
          className="pd-btn pd-btn--outline"
          onClick={() => {
            playClick();
            leaveSolo();
          }}
        >
          {t('back')}
        </button>
      </div>
    );
  } else if (phase === 'playing' && matchInfo && round) {
    content = (
      <Suspense
        fallback={
          <div className="pd-frame">
            <p className="pd-status">{t('loadingDuel')}</p>
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
        serverUp={serverUp}
        rankings={rankings}
        session={session}
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
