import type {
  ErrorPayload,
  MatchEndPayload,
  MatchStartPayload,
  RoundEndPayload,
  RoundStartPayload,
} from './types.js';

/**
 * Raw-WebSocket protocol for the Cloudflare Worker backend.
 * JSON text frames: `{ t: <message-name>, ...payload }`.
 * Routing is in the WS URL (/ws/quickplay, /ws/room/new, /ws/room/<CODE>);
 * the socket itself carries a single player session (token in query).
 */

// Client → server. Fire-and-forget; failures come back as `game:error`.
export interface WsClientMessage {
  t: 'round:submit';
  expression: string;
}

// Server → client. Same `t` names as the legacy socket.io events
// so the client state machine maps 1:1.
export type WsServerMessage =
  | { t: 'game:queued'; position: number }
  | { t: 'match:found'; roomId: string; token: string; opponentName: string }
  | { t: 'room:code'; code: string }
  | ({ t: 'match:start' } & MatchStartPayload)
  | ({ t: 'round:start' } & RoundStartPayload)
  | ({ t: 'round:end' } & RoundEndPayload)
  | ({ t: 'match:end' } & MatchEndPayload)
  | ({ t: 'game:error' } & ErrorPayload);
