import { io, type Socket } from 'socket.io-client';
import type { ClientToServerEvents, ServerToClientEvents } from '@plusduel/shared';

export type GameSocket = Socket<ServerToClientEvents, ClientToServerEvents>;

// Same-origin by default; override with VITE_SOCKET_URL=<https://server.example.com>
// when the Socket.io server lives on a different origin than the web build.
const socketUrl: string | undefined = import.meta.env.VITE_SOCKET_URL as string | undefined;

export const socket: GameSocket = io(socketUrl ?? '/', {
  autoConnect: false,
  transports: ['websocket', 'polling'],
});

/** Attach (or clear) the Supabase JWT used by the server auth middleware. */
export function setSocketToken(token: string | null): void {
  socket.auth = token ? { token } : {};
}