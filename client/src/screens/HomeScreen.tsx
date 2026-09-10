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
  const [showRules, setShowRules] = useState(false);

  return (
    <div className="pd-frame">
      <div className="pd-logo">
        <span className="pd-logo__plus">Plus</span>
        <span className="pd-logo__dual">Duel</span>
      </div>

      {error && <div className="pd-status">{error}</div>}

      {queued ? (
        <div className="pd-panel">
          {queuePos === -1 ? (
            <p className="pd-status">Room open — share your code.</p>
          ) : (
            <p className="pd-status">Looking for an opponent…</p>
          )}
          <button className="pd-btn pd-btn--primary" onClick={onCancelQueue}>
            Cancel
          </button>
        </div>
      ) : (
        <>
          <div className="pd-panel">
            <div className="pd-field">
              <label className="pd-label" htmlFor="pd-name">
                Nickname
              </label>
              <input
                id="pd-name"
                className="pd-input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Anonymous"
                maxLength={20}
                autoComplete="off"
              />
            </div>

            <button className="pd-btn pd-btn--primary" onClick={onQuickPlay}>
              Quick play
            </button>
          </div>

          <hr className="pd-divider" />

          <div className="pd-panel">
            <button className="pd-btn pd-btn--outline" onClick={onCreatePrivate}>
              Create a private room
            </button>

            <div className="pd-field">
              <input
                className="pd-input"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                placeholder="Code"
                maxLength={6}
                autoComplete="off"
                aria-label="Room code"
              />
              <button
                className="pd-btn pd-btn--outline pd-btn--sm"
                disabled={code.trim().length < 4}
                onClick={() => onJoinPrivate(code)}
              >
                Join
              </button>
            </div>
          </div>

          <hr className="pd-divider" />

          <button className="pd-btn pd-btn--outline" onClick={() => setShowRules((s) => !s)}>
            {showRules ? 'Hide rules' : 'Rules'}
          </button>

          {showRules && (
            <div className="pd-panel">
              <p className="pd-status">Build the target using every digit exactly once.</p>
              <p className="pd-status">First correct answer wins the round.</p>
              <p className="pd-status">Timeout wins nobody — a sample solution is shown.</p>
              <p className="pd-status">Best of 5 — first to 3 round wins; tied after 5 → draw.</p>
              <p className="pd-status">Operators + − × ÷ ( ) ^ √ !, concatenation allowed.</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
