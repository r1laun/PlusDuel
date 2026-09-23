import { useEffect, useRef, useState } from 'react';

interface Props {
  timeLimitMs: number;
  receivedAt: number;
}

/** Server-authoritative countdown rendered from local receipt time (skew-tolerant). */
export default function TimerBar({ timeLimitMs, receivedAt }: Props) {
  const [remaining, setRemaining] = useState(timeLimitMs);
  const raf = useRef<number>(0);

  useEffect(() => {
    const tick = () => {
      setRemaining(Math.max(0, timeLimitMs - (Date.now() - receivedAt)));
      raf.current = requestAnimationFrame(tick);
    };
    raf.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf.current);
  }, [timeLimitMs, receivedAt]);

  const pct = Math.min(100, (remaining / timeLimitMs) * 100);
  const urgent = remaining < 10_000;

  return (
    <div className="timer">
      <div className={`timer-track${urgent ? ' urgent' : ''}`}>
        <div className="timer-fill" style={{ width: `${pct}%` }} />
      </div>
      <span className="timer-num">{(remaining / 1000).toFixed(1)}s</span>
    </div>
  );
}
