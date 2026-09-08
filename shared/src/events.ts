import type {
  ErrorPayload,
  MatchEndPayload,
  MatchStartPayload,
  PlayerPublic,
  RoundEndPayload,
  RoundStartPayload,
} from './types.js';

export interface ClientToServerEvents {
  'game:queue_join': (p: { name: string }) => void;
  'game:queue_leave': () => void;
  'game:create_private': (p: { name: string }, ack?: (res: { code: string }) => void) => void;
  'game:join_private': (
    p: { name: string; code: string },
    ack?: (res: { joined: boolean }) => void,
  ) => void;
  'round:submit': (p: { expression: string }, ack?: (res: { received: boolean }) => void) => void;
}

export interface ServerToClientEvents {
  'game:queued': (p: { position: number }) => void;
  'match:start': (p: MatchStartPayload) => void;
  'round:start': (p: RoundStartPayload) => void;
  'round:end': (p: RoundEndPayload) => void;
  'match:end': (p: MatchEndPayload) => void;
  'opponent:left': () => void;
  'game:error': (p: ErrorPayload) => void;
}

export type Intersect<T, U> = Pick<T, Extract<keyof T, keyof U>> & Pick<U, Extract<keyof U, keyof T>>;

export const PLAYER_VIEW = (p: PlayerPublic): PlayerPublic => ({ id: p.id, name: p.name });
