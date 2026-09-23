import { useEffect, useRef, useState } from 'react';

interface Props {
  timeLimitMs: number;
  receivedAt: number;
}

/**
 * Server-authoritative countdown rendered from local receipt time (skew-tolerant).
 * Uses setInterval instead of requestAnimationFrame: rAF pauses in background
 * tabs, freezing a player's visible timer; intervals keep ticking (≥1s throttle).
 */
export default function TimerBar({ timeLimitMs, receivedAt }: Props) {
  const [remaining, setRemaining] = useState(timeLimitMs);
  const interval = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    setRemaining(timeLimitMs);
    const tick = () => {
      setRemaining(Math.max(0, timeLimitMs - (Date.now() - receivedAt)));
    };
    tick();
    interval.current = setInterval(tick, 100);
    // Mobile browsers (iOS Safari) suspend background timers entirely; recompute
    // immediately when the tab becomes visible so the bar never looks frozen.
    const onVisible = () => {
      if (!document.hidden) tick();
    };
    document.addEventListener('visibilitychange', onVisible);
    window.addEventListener('pageshow', onVisible);
    return () => {
      if (interval.current) clearInterval(interval.current);
      document.removeEventListener('visibilitychange', onVisible);
      window.removeEventListener('pageshow', onVisible);
    };
  }, [timeLimitMs, receivedAt]);

  return (
    <div className="pd-display pd-display--accent">{(remaining / 1000).toFixed(1)}s</div>
  );
}
