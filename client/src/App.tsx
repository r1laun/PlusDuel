import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import type {
  MatchEndPayload,
  MatchStartPayload,
  RoundEndPayload,
  RoundStartPayload,
  Session,
  Snapshot,
} from '@plusduel/shared';
import { api, JoinFailed } from './api';
import HomeScreen from './screens/HomeScreen';
import PlayScreen from './screens/PlayScreen';
import EndScreen from './screens/EndScreen';

type Phase = 'home' | 'queued' | 'playing' | 'ended';

const POLL_MS = 1000;

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

  const sessionRef = useRef<Session | null>(null);
  const myId = useRef<string>('');
  const pollTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const lastRoundIndex = useRef(0);

  useEffect(() => {
    localStorage.setItem('pd:name', name);
  }, [name]);

  const stopPolling = () => {
    if (pollTimer.current) clearInterval(pollTimer.current);
    pollTimer.current = null;
  };

  useEffect(() => stopPolling, []);

  /** Apply a server snapshot to local state. Returns false when the session died. */
  const applySnapshot = (snap: Snapshot): boolean => {
    switch (snap.kind) {
      case 'idle':
        sessionRef.current = null;
        stopPolling();
        setPhase('home');
        setRound(null);
        setRoundEnd(null);
        setMatchEnd(null);
        setMatchInfo(null);
        setError(snap.message ?? 'Session expired. Try again.');
        return false;
      case 'queued':
        setQueuePos(snap.position);
        setPhase('queued');
        return true;
      case 'waiting':
        setQueuePos(-1);
        setPhase('queued');
        return true;
      case 'playing': {
        myId.current = snap.match.youAre;
        setMatchInfo(snap.match);
        setMatchEnd(null);
        if (snap.round.index !== lastRoundIndex.current) {
          lastRoundIndex.current = snap.round.index;
          setRound(snap.round);
          setRoundEnd(null);
          setRoundReceivedAt(Date.now());
          setError('');
        } else {
          setRound(snap.round);
          setRoundEnd(snap.roundEnd);
        }
        setPhase('playing');
        return true;
      }
      case 'ended':
        stopPolling();
        if (snap.match) {
          myId.current = snap.match.youAre;
          setMatchInfo(snap.match);
        }
        setMatchEnd(snap.result);
        setPhase('ended');
        return true;
    }
  };

  const pollOnce = async () => {
    const s = sessionRef.current;
    if (!s) return;
    try {
      applySnapshot(await api.state(s));
    } catch {
      // Transient network error — keep polling, don't nuke the session.
    }
  };

  const startPolling = () => {
    stopPolling();
    pollTimer.current = setInterval(pollOnce, POLL_MS);
  };

  const resetMatchState = () => {
    lastRoundIndex.current = 0;
    setRound(null);
    setRoundEnd(null);
    setMatchEnd(null);
    setMatchInfo(null);
  };

  const quickPlay = async () => {
    setError('');
    resetMatchState();
    setPhase('queued');
    setQueuePos(0);
    try {
      const res = await api.quickplay(name);
      sessionRef.current = res.session;
      applySnapshot(res.snapshot);
      startPolling();
    } catch {
      setPhase('home');
      setError('Could not reach the server. Try again.');
    }
  };

  const createPrivate = async () => {
    setError('');
    resetMatchState();
    setPhase('queued');
    try {
      const res = await api.createRoom(name);
      sessionRef.current = res.session;
      applySnapshot(res.snapshot);
      navigator.clipboard?.writeText(res.code).catch(() => {});
      setError(`Room code ${res.code} copied to clipboard — share it!`);
      startPolling();
    } catch {
      setPhase('home');
      setError('Could not reach the server. Try again.');
    }
  };

  const joinPrivate = async (code: string) => {
    setError('');
    resetMatchState();
    setPhase('queued');
    try {
      const res = await api.joinRoom(name, code);
      sessionRef.current = res.session;
      applySnapshot(res.snapshot);
      startPolling();
    } catch (e) {
      setPhase('home');
      setError(e instanceof JoinFailed ? e.message : 'Could not reach the server. Try again.');
    }
  };

  const submitExpression = async (expression: string) => {
    const s = sessionRef.current;
    if (!s) return;
    try {
      const res = await api.submit(s, expression);
      if (!res.accepted && res.message) setError(res.message);
      applySnapshot(res.snapshot);
    } catch {
      setError('Submit failed — check your connection.');
    }
  };

  const leaveMatch = () => {
    const s = sessionRef.current;
    sessionRef.current = null;
    stopPolling();
    if (s) api.leave(s).catch(() => {});
    setPhase('home');
    resetMatchState();
    setError('');
  };

  const backHome = () => {
    leaveMatch();
  };

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
        onCancelQueue={leaveMatch}
      />
    );
  }

  return <div className="pd-app">{content}</div>;
}
