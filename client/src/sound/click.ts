/**
 * Warm acoustic-style procedural sound engine ported from PlusMath
 * (Marimba, Kalimba, Celesta & Ambient Pad).
 *
 * Uses raised-cosine envelopes and soft harmonic overtones rendered into
 * Web Audio API AudioBuffers so every tap, chime, and melody feels warm,
 * smooth, and completely free of harsh 8-bit square/sawtooth buzz or clicks.
 */

import { safeGet, safeSet } from '../storage';

type Timbre =
  /** Warm wooden marimba/kalimba tap with fast 4th-harmonic transient and pure sine body. */
  | 'WOOD_TAP'
  /** Soft celesta / Rhodes electric piano bell with gentle 2nd & 3rd harmonics. */
  | 'SOFT_CHIME'
  /** Muted, rounded warm bass/low-mid tone for backspace or gentle error feedback. */
  | 'MUTED_WARM'
  /** Silky slow-attack ambient pad for background music. */
  | 'WARM_PAD';

interface Note {
  timbre: Timbre;
  freqHz: number;
  startOffsetSec: number;
  durationSec: number;
  volume?: number;
}

const SOUND_KEY = 'pd:sound';
const MUSIC_KEY = 'pd:music';
const HAPTICS_KEY = 'pd:haptics';

let soundEnabled = safeGet(SOUND_KEY) !== '0';
let musicEnabled = safeGet(MUSIC_KEY) !== '0';
let hapticsEnabled = safeGet(HAPTICS_KEY) !== '0';
let inMenuScreen = true;

const listeners = new Set<() => void>();
function notify(): void {
  for (const fn of listeners) fn();
}

export function subscribeAudio(fn: () => void): () => void {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

export function getAudioSettings(): {
  soundEnabled: boolean;
  musicEnabled: boolean;
  hapticsEnabled: boolean;
} {
  return { soundEnabled, musicEnabled, hapticsEnabled };
}

export function setSoundEnabled(val: boolean): void {
  soundEnabled = val;
  safeSet(SOUND_KEY, val ? '1' : '0');
  notify();
}

export function setMusicEnabled(val: boolean): void {
  musicEnabled = val;
  safeSet(MUSIC_KEY, val ? '1' : '0');
  updateMusicState();
  notify();
}

export function setHapticsEnabled(val: boolean): void {
  hapticsEnabled = val;
  safeSet(HAPTICS_KEY, val ? '1' : '0');
  notify();
}

export function toggleAllAudio(): boolean {
  const next = !(soundEnabled || musicEnabled);
  soundEnabled = next;
  musicEnabled = next;
  safeSet(SOUND_KEY, next ? '1' : '0');
  safeSet(MUSIC_KEY, next ? '1' : '0');
  if (next) playClick();
  updateMusicState();
  notify();
  return next;
}

export function setInMenuScreen(inMenu: boolean): void {
  if (inMenuScreen === inMenu) return;
  inMenuScreen = inMenu;
  updateMusicState();
}

let ctx: AudioContext | null = null;
const bufferCache = new Map<string, AudioBuffer>();

function audio(): AudioContext | null {
  try {
    if (ctx) {
      if (ctx.state === 'suspended') void ctx.resume();
      return ctx;
    }
    const AC =
      window.AudioContext ??
      (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AC) return null;
    ctx = new AC();
    return ctx;
  } catch {
    return null;
  }
}

function vibrate(pattern: number | number[]): void {
  if (!hapticsEnabled) return;
  try {
    if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
      navigator.vibrate(pattern);
    }
  } catch {
    /* ignore */
  }
}

function renderNotesToBuffer(
  ac: AudioContext,
  notes: Note[],
  smoothLoopEdges = false,
): AudioBuffer {
  const sampleRate = ac.sampleRate;
  let maxEnd = 0;
  for (const n of notes) {
    const end = n.startOffsetSec + n.durationSec;
    if (end > maxEnd) maxEnd = end;
  }
  const totalDuration = maxEnd + 0.04;
  const totalSamples = Math.max(512, Math.floor(totalDuration * sampleRate));
  const audioBuf = ac.createBuffer(1, totalSamples, sampleRate);
  const out = audioBuf.getChannelData(0);

  for (const note of notes) {
    const startSample = Math.floor(note.startOffsetSec * sampleRate);
    const noteSamples = Math.max(1, Math.floor(note.durationSec * sampleRate));
    const vol = note.volume ?? 0.1;

    const attackSec =
      note.timbre === 'WARM_PAD'
        ? 0.12
        : note.timbre === 'SOFT_CHIME'
          ? 0.008
          : note.timbre === 'WOOD_TAP'
            ? 0.004
            : 0.007;
    const releaseSec = Math.max(0.012, note.durationSec * 0.25);
    const decayRate =
      note.timbre === 'WOOD_TAP'
        ? 5.2
        : note.timbre === 'SOFT_CHIME'
          ? 3.4
          : note.timbre === 'MUTED_WARM'
            ? 4.0
            : 1.1;

    for (let i = 0; i < noteSamples; i++) {
      const idx = startSample + i;
      if (idx >= totalSamples) break;
      const t = i / sampleRate;
      const p = Math.min(1, Math.max(0, t / note.durationSec));

      const attackEnv =
        t < attackSec ? 0.5 * (1 - Math.cos(Math.PI * (t / attackSec))) : 1;
      const timeRemaining = note.durationSec - t;
      const releaseEnv =
        timeRemaining < releaseSec
          ? 0.5 * (1 - Math.cos(Math.PI * Math.min(1, Math.max(0, timeRemaining / releaseSec))))
          : 1;

      const envelope = attackEnv * releaseEnv * Math.exp(-decayRate * p);
      const w = 2 * Math.PI * note.freqHz * t;

      let rawWave: number;
      switch (note.timbre) {
        case 'WOOD_TAP':
          rawWave = Math.sin(w) + 0.16 * Math.exp(-16 * p) * Math.sin(3 * w);
          break;
        case 'SOFT_CHIME':
          rawWave =
            Math.sin(w) +
            0.2 * Math.exp(-5 * p) * Math.sin(2 * w) +
            0.05 * Math.exp(-9 * p) * Math.sin(3 * w);
          break;
        case 'MUTED_WARM':
          rawWave = 0.85 * Math.sin(w) + 0.15 * Math.sin(0.5 * w);
          break;
        case 'WARM_PAD':
          rawWave =
            0.6 * Math.sin(w) + 0.25 * Math.sin(w * 1.002) + 0.15 * Math.sin(2 * w);
          break;
      }

      out[idx]! += rawWave * envelope * vol;
    }
  }

  const edgeSamples = smoothLoopEdges
    ? Math.min(Math.floor(totalSamples / 4), Math.floor(0.08 * sampleRate))
    : 0;

  for (let i = 0; i < totalSamples; i++) {
    let v = out[i]!;
    if (edgeSamples > 0) {
      if (i < edgeSamples) {
        v *= 0.5 * (1 - Math.cos((Math.PI * i) / edgeSamples));
      } else if (i > totalSamples - edgeSamples) {
        const rem = Math.min(1, Math.max(0, (totalSamples - i) / edgeSamples));
        v *= 0.5 * (1 - Math.cos(Math.PI * rem));
      }
    }
    out[i] = Math.max(-0.95, Math.min(0.95, v));
  }

  return audioBuf;
}

function playSequence(cacheKey: string, notes: Note[]): void {
  if (!soundEnabled || notes.length === 0) return;
  try {
    const ac = audio();
    if (!ac) return;
    updateMusicState();
    let buf = bufferCache.get(cacheKey);
    if (!buf) {
      buf = renderNotesToBuffer(ac, notes, false);
      bufferCache.set(cacheKey, buf);
    }
    const src = ac.createBufferSource();
    src.buffer = buf;
    src.connect(ac.destination);
    src.start();
  } catch {
    /* silent - sound is decorative */
  }
}

/**
 * Soothing, dreamy ambient puzzle soundtrack (Fmaj9 -> Em7 -> Dm9 -> Cmaj9)
 * ported from PlusMath. Gentle Rhodes/celesta notes floating over a warm sub-pad.
 */
function buildAmbientPhrase(): Note[] {
  const progressions: Array<[number, number[]]> = [
    [174.61, [440.0, 523.25, 659.25, 783.99, 659.25, 523.25]],
    [164.81, [392.0, 493.88, 587.33, 783.99, 587.33, 493.88]],
    [146.83, [349.23, 440.0, 523.25, 659.25, 523.25, 440.0]],
    [130.81, [329.63, 392.0, 493.88, 587.33, 493.88, 392.0]],
  ];
  const stepSec = 0.52;
  let t = 0.05;
  const notes: Note[] = [];
  for (const [bassFreq, arp] of progressions) {
    const barDur = stepSec * arp.length;
    notes.push({ timbre: 'WARM_PAD', freqHz: bassFreq, startOffsetSec: t, durationSec: barDur * 0.98, volume: 0.022 });
    notes.push({ timbre: 'WARM_PAD', freqHz: bassFreq * 1.5, startOffsetSec: t, durationSec: barDur * 0.98, volume: 0.014 });
    for (const freq of arp) {
      notes.push({ timbre: 'SOFT_CHIME', freqHz: freq, startOffsetSec: t, durationSec: stepSec * 2.1, volume: 0.024 });
      t += stepSec;
    }
  }
  return notes;
}

let musicSource: AudioBufferSourceNode | null = null;
let musicGain: GainNode | null = null;

function updateMusicState(): void {
  try {
    const visible = typeof document === 'undefined' || !document.hidden;
    const shouldPlay = musicEnabled && inMenuScreen && visible;
    if (!ctx) return;

    if (shouldPlay) {
      if (!musicSource || !musicGain) {
        let buf = bufferCache.get('ambient_loop');
        if (!buf) {
          buf = renderNotesToBuffer(ctx, buildAmbientPhrase(), true);
          bufferCache.set('ambient_loop', buf);
        }
        const gain = ctx.createGain();
        gain.gain.setValueAtTime(0.0001, ctx.currentTime);
        gain.connect(ctx.destination);

        const src = ctx.createBufferSource();
        src.buffer = buf;
        src.loop = true;
        src.connect(gain);
        src.start();

        musicSource = src;
        musicGain = gain;
      }
      musicGain.gain.setTargetAtTime(1.0, ctx.currentTime, 0.25);
    } else if (musicGain && ctx) {
      musicGain.gain.setTargetAtTime(0.0001, ctx.currentTime, 0.18);
    }
  } catch {
    /* ignore */
  }
}

if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => updateMusicState());
}

const PENTATONIC_KEY_FREQS = [523.25, 587.33, 659.25, 783.99, 659.25, 587.33];
let keyPitchStep = 0;

/** Soft wooden marimba tap for navigation & UI buttons. */
export function playClick(): void {
  vibrate(8);
  playSequence('click', [
    { timbre: 'WOOD_TAP', freqHz: 523.25, startOffsetSec: 0, durationSec: 0.075, volume: 0.11 },
  ]);
}

/** Warm kalimba pluck for digit & operator tiles with subtle musical progression. */
export function playKey(): void {
  vibrate(7);
  const idx = keyPitchStep++ % PENTATONIC_KEY_FREQS.length;
  const freq = PENTATONIC_KEY_FREQS[idx]!;
  playSequence(`key_${idx}`, [
    { timbre: 'WOOD_TAP', freqHz: freq, startOffsetSec: 0, durationSec: 0.095, volume: 0.12 },
  ]);
}

/** Gentle low wooden tap for Backspace / Clear. */
export function playDelete(): void {
  vibrate(8);
  playSequence('delete', [
    { timbre: 'MUTED_WARM', freqHz: 392.0, startOffsetSec: 0, durationSec: 0.065, volume: 0.1 },
    { timbre: 'MUTED_WARM', freqHz: 329.63, startOffsetSec: 0.035, durationSec: 0.08, volume: 0.1 },
  ]);
}

/** Pleasant two-note celesta chime when pressing Submit. */
export function playSubmit(): void {
  vibrate(12);
  playSequence('submit', [
    { timbre: 'SOFT_CHIME', freqHz: 523.25, startOffsetSec: 0, durationSec: 0.16, volume: 0.12 },
    { timbre: 'SOFT_CHIME', freqHz: 659.25, startOffsetSec: 0.055, durationSec: 0.2, volume: 0.13 },
  ]);
}

/** Shimmering celesta bell chord when equipping a style or unlocking an item. */
export function playStyleEquip(): void {
  vibrate(10);
  playSequence('equip', [
    { timbre: 'SOFT_CHIME', freqHz: 587.33, startOffsetSec: 0, durationSec: 0.24, volume: 0.1 },
    { timbre: 'SOFT_CHIME', freqHz: 880.0, startOffsetSec: 0.06, durationSec: 0.26, volume: 0.11 },
    { timbre: 'SOFT_CHIME', freqHz: 1174.66, startOffsetSec: 0.12, durationSec: 0.34, volume: 0.12 },
  ]);
}

/** Soft, non-jarring muted wood interval when an expression isn't right yet. */
export function playMistake(): void {
  vibrate([24, 35, 32]);
  playSequence('mistake', [
    { timbre: 'MUTED_WARM', freqHz: 349.23, startOffsetSec: 0, durationSec: 0.14, volume: 0.11 },
    { timbre: 'MUTED_WARM', freqHz: 293.66, startOffsetSec: 0.1, durationSec: 0.2, volume: 0.11 },
  ]);
}

/** Round won - bright celesta arpeggio. */
export function playRoundWin(): void {
  vibrate([16, 30, 26]);
  playSequence('round_win', [
    { timbre: 'SOFT_CHIME', freqHz: 523.25, startOffsetSec: 0, durationSec: 0.32, volume: 0.11 },
    { timbre: 'SOFT_CHIME', freqHz: 659.25, startOffsetSec: 0.07, durationSec: 0.3, volume: 0.11 },
    { timbre: 'SOFT_CHIME', freqHz: 783.99, startOffsetSec: 0.14, durationSec: 0.36, volume: 0.12 },
  ]);
}

/** Round lost / timeout - warm descending Rhodes chord. */
export function playRoundLose(): void {
  vibrate([28, 40, 38]);
  playSequence('round_lose', [
    { timbre: 'MUTED_WARM', freqHz: 392.0, startOffsetSec: 0, durationSec: 0.18, volume: 0.11 },
    { timbre: 'MUTED_WARM', freqHz: 349.23, startOffsetSec: 0.11, durationSec: 0.18, volume: 0.11 },
    { timbre: 'MUTED_WARM', freqHz: 293.66, startOffsetSec: 0.22, durationSec: 0.28, volume: 0.12 },
  ]);
}

/** Match victory - lush, resonant Cmaj9 celesta harp arpeggio from PlusMath. */
export function playMatchWin(): void {
  vibrate([18, 35, 30]);
  playSequence('match_win', [
    { timbre: 'SOFT_CHIME', freqHz: 523.25, startOffsetSec: 0.0, durationSec: 0.42, volume: 0.11 },
    { timbre: 'SOFT_CHIME', freqHz: 659.25, startOffsetSec: 0.07, durationSec: 0.38, volume: 0.11 },
    { timbre: 'SOFT_CHIME', freqHz: 783.99, startOffsetSec: 0.14, durationSec: 0.36, volume: 0.11 },
    { timbre: 'SOFT_CHIME', freqHz: 987.77, startOffsetSec: 0.21, durationSec: 0.36, volume: 0.11 },
    { timbre: 'SOFT_CHIME', freqHz: 1046.5, startOffsetSec: 0.28, durationSec: 0.5, volume: 0.13 },
  ]);
}

/** Match defeat - warm low Rhodes cadence. */
export function playMatchLose(): void {
  vibrate([28, 40, 38]);
  playSequence('match_lose', [
    { timbre: 'MUTED_WARM', freqHz: 349.23, startOffsetSec: 0.0, durationSec: 0.2, volume: 0.11 },
    { timbre: 'MUTED_WARM', freqHz: 293.66, startOffsetSec: 0.13, durationSec: 0.2, volume: 0.11 },
    { timbre: 'MUTED_WARM', freqHz: 261.63, startOffsetSec: 0.26, durationSec: 0.34, volume: 0.12 },
  ]);
}

/** Draw - balanced celesta fifth interval. */
export function playMatchDraw(): void {
  vibrate(15);
  playSequence('match_draw', [
    { timbre: 'SOFT_CHIME', freqHz: 440.0, startOffsetSec: 0.0, durationSec: 0.22, volume: 0.1 },
    { timbre: 'SOFT_CHIME', freqHz: 523.25, startOffsetSec: 0.12, durationSec: 0.28, volume: 0.1 },
  ]);
}

/** Wrap a click handler so it plays a warm marimba tap first: onClick={tap(onQuickPlay)}. */
export function tap<T extends unknown[]>(fn: (...args: T) => void): (...args: T) => void {
  return (...args: T) => {
    playClick();
    fn(...args);
  };
}
