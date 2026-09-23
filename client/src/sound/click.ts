/**
 * Tiny synthesized game sounds (Web Audio API, no assets).
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

type Wave = OscillatorType;

/** One note: frequency, start offset (s), duration (s). */
function tone(ac: AudioContext, wave: Wave, freq: number, at: number, dur: number, vol: number): void {
  const t = ac.currentTime + at;
  const osc = ac.createOscillator();
  const gain = ac.createGain();
  osc.type = wave;
  osc.frequency.setValueAtTime(freq, t);
  gain.gain.setValueAtTime(vol, t);
  gain.gain.exponentialRampToValueAtTime(0.0001, t + dur);
  osc.connect(gain).connect(ac.destination);
  osc.start(t);
  osc.stop(t + dur + 0.01);
}

function play(seq: Array<[Wave, number, number, number]>, vol = 0.06): void {
  try {
    const ac = audio();
    if (!ac) return;
    for (const [wave, freq, at, dur] of seq) tone(ac, wave, freq, at, dur, vol);
  } catch {
    /* silent — sound is decorative */
  }
}

/** Menu buttons, toggles, copy — the default UI click. */
export function playClick(): void {
  play([['square', 660, 0, 0.06]]);
}

/** Calculator keys (digits, operators) — softer blip. */
export function playKey(): void {
  play([['triangle', 520, 0, 0.05]], 0.05);
}

/** Backspace / Clear — low blip. */
export function playDelete(): void {
  play([['square', 330, 0, 0.05]], 0.05);
}

/** Submit answer — rising two-tone confirm. */
export function playSubmit(): void {
  play([
    ['square', 660, 0, 0.06],
    ['square', 880, 0.07, 0.08],
  ]);
}

/** Round won — quick major arpeggio. */
export function playRoundWin(): void {
  play([
    ['triangle', 523, 0, 0.09],
    ['triangle', 659, 0.09, 0.09],
    ['triangle', 784, 0.18, 0.14],
  ], 0.07);
}

/** Round lost / timeout — descending tone. */
export function playRoundLose(): void {
  play([
    ['sawtooth', 330, 0, 0.1],
    ['sawtooth', 220, 0.1, 0.16],
  ], 0.05);
}

/** Match victory — bright fanfare. */
export function playMatchWin(): void {
  play([
    ['triangle', 523, 0, 0.1],
    ['triangle', 659, 0.1, 0.1],
    ['triangle', 784, 0.2, 0.1],
    ['triangle', 1046, 0.3, 0.22],
  ], 0.07);
}

/** Match defeat — low descent. */
export function playMatchLose(): void {
  play([
    ['sawtooth', 262, 0, 0.14],
    ['sawtooth', 196, 0.14, 0.14],
    ['sawtooth', 147, 0.28, 0.22],
  ], 0.05);
}

/** Draw — neutral two-tone. */
export function playMatchDraw(): void {
  play([
    ['triangle', 440, 0, 0.1],
    ['triangle', 440, 0.14, 0.14],
  ], 0.06);
}

/** Wrap a click handler so it beeps first: onClick={tap(onQuickPlay)}. */
export function tap<T extends unknown[]>(fn: (...args: T) => void): (...args: T) => void {
  return (...args: T) => {
    playClick();
    fn(...args);
  };
}
