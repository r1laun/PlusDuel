import {
  configForLevel,
  difficultyCoefficient,
  generateRound,
  roundScore,
  validateExpression,
  type ServerToClientEvents,
} from '@plusduel/shared';
import type { Socket } from 'socket.io';

export const BEST_OF = 5;
export const ROUNDS_TO_WIN = Math.ceil(BEST_OF / 2);
const BETWEEN_ROUNDS_MS = 3500;

export interface Player {
  socket: Socket<ServerToClientEvents, any>;
  name: string;
  score: number;
  roundWins: number;
}

type Phase = 'round-active' | 'between-rounds' | 'ended';

interface RoundData {
  digits: number[];
  target: number;
  solution: string;
  timeLimitMs: number;
  startedAt: number;
}

let roomSeq = 0;

export class Room {
  readonly id: string;
  readonly code: string | null;
  players: [Player, Player];
  phase: Phase = 'round-active';
  roundIndex = 0;

  private level: number;
  private roundData: RoundData | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private nextRoundTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(a: Player, b: Player, opts: { code?: string; startLevel?: number } = {}) {
    this.id = `room-${++roomSeq}`;
    this.code = opts.code ?? Math.random().toString(36).slice(2, 6).toUpperCase();
    this.players = [a, b];
    this.level = opts.startLevel ?? 1;
  }

  sideOf(socketId: string): 0 | 1 | null {
    if (this.players[0].socket.id === socketId) return 0;
    if (this.players[1].socket.id === socketId) return 1;
    return null;
  }

  start(): void {
    this.forEachPlayer((p) => {
      const other = this.other(p);
      p.socket.emit('match:start', {
        roomCode: this.code,
        opponent: { id: other.socket.id, name: other.name },
        youAre: p.socket.id,
        bestOf: BEST_OF,
        roundsToWin: ROUNDS_TO_WIN,
      });
    });
    this.beginRound();
  }

  submit(socketId: string, expression: string): void {
    if (this.phase !== 'round-active' || !this.roundData) return;
    const side = this.sideOf(socketId);
    if (side === null) return;

    // Server is source of truth — re-validate regardless of client claims.
    const result = validateExpression(expression, this.roundData.digits, this.roundData.target);
    if (!result.valid) {
      this.players[side].socket.emit('game:error', {
        message: `Invalid expression: ${result.reason}`,
      });
      return;
    }
    this.declareWinner(side, expression);
  }

  forfeit(quitterSocketId: string): void {
    if (this.phase === 'ended') return;
    const side = this.sideOf(quitterSocketId);
    const winnerSide: 0 | 1 | null = side === null ? null : ((1 - side) as 0 | 1);
    const winnerId = winnerSide === null ? null : this.players[winnerSide].socket.id;
    for (const p of this.players) {
      if (p.socket.id !== quitterSocketId || winnerId === null) {
        p.socket.emit('match:end', {
          winnerId,
          reason: 'forfeit',
          scoresByPlayer: this.scores(),
          winsByPlayer: this.wins(),
        });
      }
    }
    this.destroy();
  }

  destroy(): void {
    this.clearTimers();
    this.phase = 'ended';
  }

  private beginRound(): void {
    this.phase = 'round-active';
    this.roundIndex++;
    this.level = Math.max(1, Math.min(6, this.roundIndex));

    const gen = generateRound(this.level);
    const cfg = configForLevel(this.level);
    const timeLimitMs = cfg.timeLimitSec * 1000;
    const coefficient = difficultyCoefficient(gen.digits.length, cfg.targetMax);
    this.roundData = { digits: gen.digits, target: gen.target, solution: gen.solution, timeLimitMs, startedAt: Date.now() };

    for (const p of this.players) {
      p.socket.emit('round:start', {
        index: this.roundIndex,
        digits: gen.digits,
        target: gen.target,
        timeLimitMs,
        endsAt: this.roundData.startedAt + timeLimitMs,
        difficultyCoefficient: coefficient,
      });
    }

    this.timer = setTimeout(() => this.timeout(), timeLimitMs);
  }

  private declareWinner(winnerSide: 0 | 1, expression: string): void {
    if (this.phase !== 'round-active' || !this.roundData) return;
    const remainingMs = Math.max(0, this.roundData.startedAt + this.roundData.timeLimitMs - Date.now());
    const points = roundScore(
      this.roundData.digits.length,
      configForLevel(this.level).targetMax,
      remainingMs,
      this.roundData.timeLimitMs,
    );
    this.players[winnerSide].score += points;
    this.players[winnerSide].roundWins += 1;

    this.clearTimers();
    this.phase = 'between-rounds';

    const payload = {
      index: this.roundIndex,
      winnerId: this.players[winnerSide].socket.id,
      winningExpression: expression,
      sampleSolution: this.roundData.solution,
      scoresByPlayer: this.scores(),
      winsByPlayer: this.wins(),
    };
    for (const p of this.players) p.socket.emit('round:end', payload);

    if (this.matchOver()) {
      this.finish();
      return;
    }
    this.nextRoundTimer = setTimeout(() => this.beginRound(), BETWEEN_ROUNDS_MS);
  }

  private timeout(): void {
    if (this.phase !== 'round-active') return;
    this.clearTimers();
    this.phase = 'between-rounds';
    const payload = {
      index: this.roundIndex,
      winnerId: null,
      winningExpression: null,
      sampleSolution: this.roundData?.solution ?? null,
      scoresByPlayer: this.scores(),
      winsByPlayer: this.wins(),
    };
    for (const p of this.players) p.socket.emit('round:end', payload);
    if (this.matchOver()) {
      this.finish();
      return;
    }
    this.nextRoundTimer = setTimeout(() => this.beginRound(), BETWEEN_ROUNDS_MS);
  }

  /** Match ends at 3 round wins — or after 5 rounds (draw if tied). */
  private matchOver(): boolean {
    return (
      this.players[0].roundWins >= ROUNDS_TO_WIN ||
      this.players[1].roundWins >= ROUNDS_TO_WIN ||
      this.roundIndex >= BEST_OF
    );
  }

  private finish(): void {
    this.destroy();
    const [a, b] = this.players;
    const winner = a.roundWins === b.roundWins ? null : a.roundWins > b.roundWins ? a : b;
    for (const p of this.players) {
      p.socket.emit('match:end', {
        winnerId: winner?.socket.id ?? null,
        reason: 'rounds',
        scoresByPlayer: this.scores(),
        winsByPlayer: this.wins(),
      });
    }
  }

  private scores(): Record<string, number> {
    return { [this.players[0].socket.id]: this.players[0].score, [this.players[1].socket.id]: this.players[1].score };
  }

  private wins(): Record<string, number> {
    return { [this.players[0].socket.id]: this.players[0].roundWins, [this.players[1].socket.id]: this.players[1].roundWins };
  }

  private other(p: Player): Player {
    return p === this.players[0] ? this.players[1] : this.players[0];
  }

  private forEachPlayer(fn: (p: Player) => void): void {
    fn(this.players[0]);
    fn(this.players[1]);
  }

  private clearTimers(): void {
    if (this.timer) clearTimeout(this.timer);
    if (this.nextRoundTimer) clearTimeout(this.nextRoundTimer);
    this.timer = null;
    this.nextRoundTimer = null;
  }
}
