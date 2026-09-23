import { useState } from 'react';

interface Props {
  name: string;
  setName: (n: string) => void;
  queued: boolean;
  queuePos: number;
  error: string;
  onQuickPlay: () => void;
  onCreatePrivate: () => void;
  onJoinPrivate: (code: string) => void;
  onCancelQueue: () => void;
}

export default function HomeScreen({
  name,
  setName,
  queued,
  queuePos,
  error,
  onQuickPlay,
  onCreatePrivate,
  onJoinPrivate,
  onCancelQueue,
}: Props) {
  const [code, setCode] = useState('');

  return (
    <div className="screen home">
      <h1 className="logo-img">
        <img src="/logo-site.png" alt="PlusDuel" />
      </h1>
      <p className="site-name">
        <em>Plus</em>Duel
      </p>
      <p className="tagline">Build the target using every digit exactly once.</p>

      {error && <div className="notice">{error}</div>}

      {queued ? (
        <div className="card queue">
          {queuePos === -1 ? (
            <p className="muted">Room open — share your code.</p>
          ) : (
            <p className="muted">Looking for an opponent…</p>
          )}
          <div className="spinner" />
          <button className="btn ghost" onClick={onCancelQueue}>
            Cancel
          </button>
        </div>
      ) : (
        <div className="card">
          <label className="field">
            Nickname
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Anonymous"
              maxLength={20}
              autoComplete="off"
            />
          </label>

          <button className="btn primary full" onClick={onQuickPlay}>
            Quick play
          </button>

          <div className="divider" />

          <button className="btn full" onClick={onCreatePrivate}>
            Create a private room
          </button>

          <div className="join-row">
            <input
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              placeholder="Code"
              maxLength={6}
              className="code-input"
              autoComplete="off"
            />
            <button
              className="btn"
              disabled={code.trim().length < 4}
              onClick={() => onJoinPrivate(code)}
            >
              Join
            </button>
          </div>
        </div>
      )}
    </div>
  );
}