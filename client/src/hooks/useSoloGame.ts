import { useCallback, useEffect, useRef, useState } from 'react';
import type {
  MatchEndPayload,
  MatchStartPayload,
  RoundEndPayload,
  RoundStartPayload,
} from '@plusduel/shared';
import {
  SOLO_BETWEEN_ROUNDS_MS,
  SOLO_CLOCK,
  SOLO_ROUNDS_TO_WIN,
  SOLO_YOU,
  createSoloRound,
  emptyScores,
  levelForMatchRound,
  matchEndPayload,
  roundEndPayload,
  roundStartPayload,
  scoreSoloRound,
  soloMatchInfo,
  validateSoloExpression,
  type SoloMode,
  type SoloRound,
} from '../solo/engine';

export interface SoloStats {
  solved: number;
  failed: number;
  score: number;
}

interface SoloState {
  active: boolean;
  mode: SoloMode;
  matchInfo: MatchStartPayload | null;
  round: RoundStartPayload | null;
  roundReceivedAt: number;
  roundEnd: RoundEndPayload | null;
  matchEnd: MatchEndPayload | null;
  error: string;
  stats: SoloStats;
}

/**
 * Fully local single-player game — no socket. Mirrors the server Room flow
 * (round → pause → next, best-of-5 in match mode) and exposes the same
 * payload shapes so PlayScreen/EndScreen are reused as-is.
 */
export function useSoloGame() {
  const [state, setState] = useState<SoloState>({
    active: false,
    mode: 'endless',
    matchInfo: null,
    round: null,
    roundReceivedAt: 0,
    roundEnd: null,
    matchEnd: null,
    error: '',
    stats: { solved: 0, failed: 0, score: 0 },
  });

  const timers = useRef<ReturnType<typeof setTimeout>[]>([]);
  // Mutable game data lives in a ref — timer callbacks must see fresh values.
  const game = useRef<{
    mode: SoloMode;
    level: number;
    scores: Record<string, number>;
    wins: Record<string, number>;
    failed: number;
    current: SoloRound | null;
  } | null>(null);

  const clearTimers = useCallback(() => {
    for (const t of timers.current) clearTimeout(t);
    timers.current = [];
  }, []);

  useEffect(() => clearTimers, [clearTimers]);

  const later = useCallback((fn: () => void, ms: number) => {
    timers.current.push(setTimeout(fn, ms));
  }, []);

  const push = useCallback((patch: Partial<SoloState>) => {
    setState((s) => ({ ...s, ...patch }));
  }, []);

  const beginRound = useCallback(
    (index: number) => {
      const g = game.current;
      if (!g) return;
      const level = g.mode === 'match' ? levelForMatchRound(index) : g.level;
      push({ error: '', roundEnd: null });
      void createSoloRound(index, level).then((r) => {
        // Game may have been left while the generator chunk was loading.
        if (game.current !== g) return;
        g.current = r;
        push({
          active: true,
          round: roundStartPayload(r),
          roundReceivedAt: Date.now(),
          roundEnd: null,
          error: '',
        });
        timers.current.push(
          setTimeout(() => {
            const gg = game.current;
            if (!gg || gg.current !== r) return;
            gg.wins[SOLO_CLOCK] = (gg.wins[SOLO_CLOCK] ?? 0) + 1;
            gg.failed += 1;
            const end = roundEndPayload(r, SOLO_CLOCK, null, gg.scores, gg.wins);
            if (gg.mode === 'endless') {
              push({
                roundEnd: end,
                stats: { solved: gg.wins[SOLO_YOU] ?? 0, failed: gg.failed, score: gg.scores[SOLO_YOU] ?? 0 },
              });
              later(() => beginRoundRef.current(r.index + 1), SOLO_BETWEEN_ROUNDS_MS);
            } else {
              advanceMatchRef.current(gg, r, end);
            }
          }, r.timeLimitMs),
        );
      });
    },
    [later, push],
  );
  const beginRoundRef = useRef(beginRound);
  beginRoundRef.current = beginRound;

  const advanceMatch = useCallback(
    (g: NonNullable<typeof game.current>, r: SoloRound, end: RoundEndPayload) => {
      const you = g.wins[SOLO_YOU] ?? 0;
      const clock = g.wins[SOLO_CLOCK] ?? 0;
      const over =
        you >= SOLO_ROUNDS_TO_WIN || clock >= SOLO_ROUNDS_TO_WIN || r.index >= 5;
      if (over) {
        const winner = you === clock ? null : you > clock ? SOLO_YOU : SOLO_CLOCK;
        push({ roundEnd: end, matchEnd: matchEndPayload(winner, g.scores, g.wins) });
        return;
      }
      push({
        roundEnd: end,
        stats: { solved: you, failed: g.failed, score: g.scores[SOLO_YOU] ?? 0 },
      });
      later(() => beginRoundRef.current(r.index + 1), SOLO_BETWEEN_ROUNDS_MS);
    },
    [later, push],
  );
  const advanceMatchRef = useRef(advanceMatch);
  advanceMatchRef.current = advanceMatch;

  const start = useCallback(
    (mode: SoloMode, level: number) => {
      clearTimers();
      game.current = { mode, level, scores: emptyScores(), wins: emptyScores(), failed: 0, current: null };
      push({
        active: true,
        mode,
        matchInfo: soloMatchInfo(),
        round: null,
        roundEnd: null,
        matchEnd: null,
        error: '',
        stats: { solved: 0, failed: 0, score: 0 },
      });
      beginRoundRef.current(1);
    },
    [clearTimers, push],
  );

  const submit = useCallback(
    (expression: string) => {
      const g = game.current;
      const r = g?.current;
      if (!g || !r) return;
      void validateSoloExpression(expression, r.digits, r.target).then((res) => {
        // Round may have timed out while the validator chunk was loading.
        if (game.current !== g || g.current !== r) return;
        if (!res.valid) {
          push({ error: `Invalid expression: ${res.reason}` });
          return;
        }
        clearTimers();
        const remainingMs = Math.max(0, r.startedAt + r.timeLimitMs - Date.now());
        const level = g.mode === 'match' ? levelForMatchRound(r.index) : g.level;
        void scoreSoloRound(level, r.digits.length, remainingMs, r.timeLimitMs).then((points) => {
          if (game.current !== g || g.current !== r) return;
          g.scores[SOLO_YOU] = (g.scores[SOLO_YOU] ?? 0) + points;
          g.wins[SOLO_YOU] = (g.wins[SOLO_YOU] ?? 0) + 1;
          const end = roundEndPayload(r, SOLO_YOU, expression, g.scores, g.wins);
          if (g.mode === 'endless') {
            push({
              roundEnd: end,
              stats: { solved: g.wins[SOLO_YOU] ?? 0, failed: g.failed, score: g.scores[SOLO_YOU] ?? 0 },
            });
            later(() => beginRoundRef.current(r.index + 1), SOLO_BETWEEN_ROUNDS_MS);
          } else {
            advanceMatchRef.current(g, r, end);
          }
        });
      });
    },
    [clearTimers, later, push],
  );

  const leave = useCallback(() => {
    clearTimers();
    game.current = null;
    push({
      active: false,
      matchInfo: null,
      round: null,
      roundEnd: null,
      matchEnd: null,
      error: '',
      stats: { solved: 0, failed: 0, score: 0 },
    });
  }, [clearTimers, push]);

  return { ...state, start, submit, leave };
}
