import type { LeaderboardEntry } from './rating.js';

export type PlayerId = string;

export interface PlayerPublic {
  id: PlayerId;
  name: string;
}

export interface RoundConfig {
  level: number;
  digits: number[];
  target: number;
  timeLimitMs: number;
  difficultyCoefficient: number;
}

export interface RoundStartPayload {
  index: number;
  digits: number[];
  target: number;
  timeLimitMs: number;
  endsAt: number;
  difficultyCoefficient: number;
}

export interface RoundEndPayload {
  index: number;
  winnerId: PlayerId | null;
  winningExpression: string | null;
  sampleSolution: string | null;
  scoresByPlayer: Record<PlayerId, number>;
  winsByPlayer: Record<PlayerId, number>;
}

export interface MatchStartPayload {
  roomCode: string | null;
  opponent: PlayerPublic;
  youAre: PlayerId;
  bestOf: number;
  roundsToWin: number;
  /** False for same-network quick-play (no Elo moves). Absent = rated/legacy. */
  rated?: boolean;
}

export interface RatingChange {
  before: number;
  after: number;
  title: string;
}

export interface MatchEndPayload {
  winnerId: PlayerId | null;
  reason: 'rounds' | 'forfeit';
  scoresByPlayer: Record<PlayerId, number>;
  winsByPlayer: Record<PlayerId, number>;
  /** Present for rated matches; keyed by socket id. */
  ratingsByPlayer?: Record<PlayerId, RatingChange>;
}

export interface RankingsPayload {
  top: LeaderboardEntry[];
  you: LeaderboardEntry | null;
}

export interface ErrorPayload {
  message: string;
}
