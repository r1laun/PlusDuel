import type { WsClientMessage, WsServerMessage } from '@plusduel/shared';

export type { WsServerMessage };

export interface NetConn {
  send: (msg: WsClientMessage) => void;
  close: () => void;
}

/**
 * Raw-WebSocket replacement for the socket.io client.
 * One NetConn = one player session on one endpoint.
 * Routing is in the URL; the server speaks JSON `{ t, ...payload }` frames.
 */
export function wsUrl(path: string, params: Record<string, string> = {}): string {
  const override = import.meta.env.VITE_SOCKET_URL as string | undefined;
  const origin = override
    ? override.replace(/^http/, 'ws')
    : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}`;
  const url = new URL(path, origin);
  for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);
  return url.toString();
}

export function connect(
  url: string,
  onMessage: (msg: WsServerMessage) => void,
  onClose: () => void,
): NetConn {
  const ws = new WebSocket(url);
  ws.onmessage = (ev) => {
    try {
      const msg = JSON.parse(String(ev.data)) as WsServerMessage;
      if (msg && typeof msg.t === 'string') onMessage(msg);
    } catch {
      /* malformed frame — ignore */
    }
  };
  ws.onclose = () => onClose();
  ws.onerror = () => {
    // onclose follows a failed upgrade; nothing to do here.
  };
  return {
    send: (msg) => {
      if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg));
    },
    close: () => ws.close(1000, 'client-leave'),
  };
}
