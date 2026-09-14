import type {
  MatchEndPayload,
  MatchStartPayload,
  PlayerId,
  RoundEndPayload,
  RoundStartPayload,
} from './types.js';

/**
 * HTTP contract between the Netlify Functions backend and the polling client.
 * Every endpoint answers with the full snapshot for the calling player,
 * so the client never needs to merge diffs — it just applies what it gets.
 */

export interface Session {
  playerId: PlayerId;
  token: string;
}

export type Snapshot =
  | { kind: 'idle'; message?: string }
  | { kind: 'queued'; position: number }
  | { kind: 'waiting'; code: string }
  | { kind: 'playing'; match: MatchStartPayload; round: RoundStartPayload; roundEnd: RoundEndPayload | null }
  | { kind: 'ended'; match: MatchStartPayload | null; result: MatchEndPayload };

export interface QuickplayRequest {
  name: string;
}

export interface RoomNewRequest {
  name: string;
}

export interface RoomJoinRequest {
  name: string;
  code: string;
}

export interface SubmitRequest {
  playerId: PlayerId;
  token: string;
  expression: string;
}

export interface LeaveRequest {
  playerId: PlayerId;
  token: string;
}

export interface SessionResponse {
  session: Session;
  snapshot: Snapshot;
}

export interface SubmitResponse {
  accepted: boolean;
  message?: string;
  snapshot: Snapshot;
}

export type JoinError = 'not-found' | 'full' | 'expired';
