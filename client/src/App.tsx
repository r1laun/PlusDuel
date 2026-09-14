import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import type {
  MatchEndPayload,
  MatchStartPayload,
  RoundEndPayload,
  RoundStartPayload,
  WsServerMessage,
} from '@plusduel/shared';
import { connect, wsUrl, type NetConn } from './net';
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
  const myId = useRef<string>('');

  // One NetConn = one player session. Swapping connRef makes the old
  // connection's close event stale and ignored (used for lobby→room handover).
  const connRef = useRef<NetConn | null>(null);
  const phaseRef = useRef<Phase>('home');
  const closeMsgRef = useRef('Connection lost. Try again.');
  useEffect(() => {
    phaseRef.current = phase;
  }, [phase]);

  const closeConn = () => {
    const c = connRef.current;
    connRef.current = null;
    c?.close();
  };

  const handleClose = (c: NetConn) => {
    if (connRef.current !== c) return; // stale (handover) — ignore
    connRef.current = null;
    if (phaseRef.current === 'queued') {
      resetMatchState();
      setPhase('home');
      setError(closeMsgRef.current);
    } else if (phaseRef.current === 'playing') {
      // Unexpected drop mid-match: the server forfeits us, opponent is notified.
      resetMatchState();
      setPhase('home');
      setError('Connection lost — match forfeited.');
    }
    // In 'ended'/'home' a close is always expected — ignore.
  };

  const handleMessage = (m: WsServerMessage) => {
    switch (m.t) {
      case 'game:queued':
        setQueuePos(m.position);
        break;
      case 'match:found': {
        // Pairing done on the lobby socket: move to the room socket.
        const room = connect(
          wsUrl(`/ws/room/id/${m.roomId}`, { token: m.token, name }),
          handleMessage,
          () => handleClose(room),
        );
        const stale = connRef.current;
        connRef.current = room;
        stale?.close();
        break;
      }
      case 'room:code':
        setQueuePos(-1);
        navigator.clipboard?.writeText(m.code).catch(() => {});
        setError(`Room code ${m.code} copied to clipboard — share it!`);
        break;
      case 'match:start': {
        const { t: _t, ...info } = m;
        myId.current = info.youAre;
        setMatchInfo(info);
        setRoundEnd(null);
        setMatchEnd(null);
        setPhase('playing');
        break;
      }
      case 'round:start': {
        const { t: _t, ...info } = m;
        setRound(info);
        setRoundEnd(null);
        setRoundReceivedAt(Date.now());
        setError('');
        break;
      }
      case 'round:end': {
        const { t: _t, ...info } = m;
        setRoundEnd(info);
        break;
      }
      case 'match:end': {
        const { t: _t, ...info } = m;
        setMatchEnd(info);
        setPhase('ended');
        break;
      }
      case 'game:error':
        setError(m.message);
        break;
    }
  };

  const openConn = (url: string) => {
    const c = connect(url, handleMessage, () => handleClose(c));
    const stale = connRef.current;
    connRef.current = c;
    stale?.close();
  };

  const resetMatchState = () => {
    setRound(null);
    setRoundEnd(null);
    setMatchEnd(null);
    setMatchInfo(null);
  };

  const quickPlay = () => {
    setError('');
    closeMsgRef.current = 'Connection lost. Try again.';
    openConn(wsUrl('/ws/quickplay', { name }));
    setPhase('queued');
  };

  const createPrivate = () => {
    setError('');
    closeMsgRef.current = 'Connection lost. Try again.';
    openConn(wsUrl('/ws/room/new', { name }));
    setPhase('queued');
    // Room code arrives via `room:code`; waiting text is shown meanwhile.
  };

  const joinPrivate = (code: string) => {
    setError('');
    // Optimistic: show the waiting screen immediately. A rejected upgrade
    // (unknown/full code) closes the socket → handleClose sends us home.
    closeMsgRef.current = `Could not join room "${code}".`;
    openConn(wsUrl(`/ws/room/${code.trim().toUpperCase()}`, { name }));
    setPhase('queued');
  };

  const submitExpression = (expression: string) => {
    connRef.current?.send({ t: 'round:submit', expression });
  };

  const leaveMatch = () => {
    // Closing notifies the server: opponent wins by forfeit (same as before).
    closeConn();
    setPhase('home');
    resetMatchState();
    setError('');
  };

  const backHome = () => {
    closeConn();
    setPhase('home');
    resetMatchState();
    setError('');
  };

  useEffect(() => {
    localStorage.setItem('pd:name', name);
  }, [name]);

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
          closeConn();
          setPhase('home');
        }}
      />
    );
  }

  return <div className="pd-app">{content}</div>;
}
