import type {
  LeaveRequest,
  QuickplayRequest,
  RoomJoinRequest,
  RoomNewRequest,
  Session,
  SessionResponse,
  Snapshot,
  SubmitRequest,
  SubmitResponse,
} from '@plusduel/shared';

export type { Session, Snapshot };

/**
 * HTTP polling replacement for the socket.io client.
 * Same-origin by default (/api/* → Netlify Function, see netlify.toml);
 * override with VITE_API_URL when the API lives elsewhere (local netlify dev).
 */
const base: string = (import.meta.env.VITE_API_URL as string | undefined) ?? '';

async function post<T>(path: string, body: unknown): Promise<{ status: number; data: T }> {
  const res = await fetch(`${base}${path}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
  const data = (await res.json().catch(() => ({}))) as T;
  return { status: res.status, data };
}

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${base}${path}`);
  if (!res.ok) throw new Error(`request failed: ${res.status}`);
  return (await res.json()) as T;
}

export class JoinFailed extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

export const api = {
  quickplay(name: string): Promise<SessionResponse> {
    return post<SessionResponse>('/api/quickplay', { name } satisfies QuickplayRequest).then((r) => r.data);
  },

  createRoom(name: string): Promise<SessionResponse & { code: string }> {
    return post<SessionResponse & { code: string }>('/api/room/new', { name } satisfies RoomNewRequest).then(
      (r) => r.data,
    );
  },

  async joinRoom(name: string, code: string): Promise<SessionResponse> {
    const r = await post<SessionResponse>('/api/room/join', { name, code } satisfies RoomJoinRequest);
    if (r.status === 404) throw new JoinFailed(`Could not join room "${code}".`, 404);
    if (r.status !== 200) throw new JoinFailed('Room unavailable.', r.status);
    return r.data;
  },

  state(session: Session): Promise<Snapshot> {
    return get<{ snapshot: Snapshot }>(
      `/api/state?playerId=${encodeURIComponent(session.playerId)}&token=${encodeURIComponent(session.token)}`,
    ).then((r) => r.snapshot);
  },

  submit(session: Session, expression: string): Promise<SubmitResponse> {
    return post<SubmitResponse>('/api/submit', {
      playerId: session.playerId,
      token: session.token,
      expression,
    } satisfies SubmitRequest).then((r) => r.data);
  },

  leave(session: Session): Promise<void> {
    return post('/api/leave', {
      playerId: session.playerId,
      token: session.token,
    } satisfies LeaveRequest).then(() => undefined);
  },
};
