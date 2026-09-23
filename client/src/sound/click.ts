/**
 * Tiny synthesized UI click (Web Audio API, no assets).
 * Created lazily on the first user gesture, so autoplay policies don't apply.
 * Everything is wrapped in try/catch — no Audio API must never break the game.
 */

let ctx: AudioContext | null = null;

function audio(): AudioContext | null {
  try {
    if (ctx) {
      if (ctx.state === 'suspended') void ctx.resume();
      return ctx;
    }
    const AC = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AC) return null;
    ctx = new AC();
    return ctx;
  } catch {
    return null;
  }
}

export function playClick(): void {  try {
    const ac = audio();
    if (!ac) return;
    const t = ac.currentTime;
    const osc = ac.createOscillator();
    const gain = ac.createGain();
    osc.type = 'square';
    osc.frequency.setValueAtTime(660, t);
    gain.gain.setValueAtTime(0.06, t);
    gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.06);
    osc.connect(gain).connect(ac.destination);
    osc.start(t);
    osc.stop(t + 0.07);
  } catch {
    /* silent — sound is decorative */
  }
}

/** Wrap a click handler so it beeps first: onClick={tap(onQuickPlay)}. */
export function tap<T extends unknown[]>(fn: (...args: T) => void): (...args: T) => void {
  return (...args: T) => {
    playClick();
    fn(...args);
  };
}
