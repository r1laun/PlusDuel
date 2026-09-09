import { createServer } from 'node:http';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Server, type Socket } from 'socket.io';
import type { ClientToServerEvents, ServerToClientEvents } from '@plusduel/shared';
import { Hub } from './hub.js';
import type { Player } from './match.js';

const PORT = Number(process.env.PORT ?? 3001);
const ORIGINS = (process.env.CORS_ORIGIN ?? 'http://localhost:5173').split(',');
// Path to the built client (client/dist). Served on the same origin so Socket.io
// needs no cross-origin setup in production.
const CLIENT_DIST = fileURLToPath(new URL('../../client/dist', import.meta.url));

type GameSocket = Socket<ClientToServerEvents, ServerToClientEvents>;

const ADJECTIVES = ['Swift', 'Clever', 'Fierce', 'Lucid', 'Prime', 'Cosmic', 'Rapid', 'Sharp'];
const NOUNS = ['Otter', 'Falcon', 'Turing', 'Panda', 'Vector', 'Comet', 'Pixel', 'Nova'];

function randomName(): string {
  const a = ADJECTIVES[Math.floor(Math.random() * ADJECTIVES.length)];
  const n = NOUNS[Math.floor(Math.random() * NOUNS.length)];
  return `${a}${n}${Math.floor(Math.random() * 90 + 10)}`;
}

function sanitizeName(name: unknown): string | null {
  if (typeof name !== 'string') return null;
  const trimmed = name.trim().slice(0, 20);
  return trimmed.length >= 2 ? trimmed : null;
}

const httpServer = createServer();
const io = new Server<ClientToServerEvents, ServerToClientEvents>(httpServer, {
  cors: { origin: ORIGINS },
});

const hub = new Hub();

io.on('connection', (socket) => {
  let name = randomName();

  const makePlayer = (): Player => ({ socket, name, score: 0, roundWins: 0 });

  socket.on('game:queue_join', ({ name: requested }) => {
    name = sanitizeName(requested) ?? randomName();
    hub.enqueue(makePlayer());
  });

  socket.on('game:queue_leave', () => {
    hub.dequeue(socket.id);
  });

  socket.on('game:create_private', ({ name: requested }, ack) => {
    name = sanitizeName(requested) ?? randomName();
    const code = hub.createPrivate(makePlayer());
    if (!code) {
      socket.emit('game:error', { message: 'Could not create room' });
      return;
    }
    ack?.({ code });
  });

  socket.on('game:join_private', ({ name: requested, code }, ack) => {
    name = sanitizeName(requested) ?? randomName();
    const joined = hub.joinPrivate(makePlayer(), code);
    if (!joined) socket.emit('game:error', { message: 'Room not found or already full' });
    ack?.({ joined });
  });

  socket.on('round:submit', ({ expression }, ack) => {
    const room = hub.roomForSocket(socket.id);
    if (!room) {
      socket.emit('game:error', { message: 'Not in an active match' });
      return;
    }
    room.submit(socket.id, expression);
    ack?.({ received: true });
  });

  socket.on('disconnect', () => {
    hub.handleDisconnect(socket.id);
  });
});

const MIME: Record<string, string> = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json',
  '.xml': 'application/xml; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
  '.map': 'application/json',
};

function serveStatic(req: import('node:http').IncomingMessage, res: import('node:http').ServerResponse) {
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    res.writeHead(405).end();
    return;
  }
  let pathname: string;
  try {
    pathname = decodeURIComponent(new URL(req.url ?? '/', 'http://localhost').pathname);
  } catch {
    res.writeHead(400).end();
    return;
  }
  if (pathname === '/') pathname = '/index.html';

  const filePath = normalize(join(CLIENT_DIST, pathname));
  if (!filePath.startsWith(CLIENT_DIST)) {
    res.writeHead(403).end();
    return;
  }

  let finalPath = filePath;
  if (existsSync(finalPath) && statSync(finalPath).isDirectory()) finalPath = join(finalPath, 'index.html');

  if (!existsSync(finalPath)) {
    // SPA fallback → index.html
    const fallback = join(CLIENT_DIST, 'index.html');
    if (!existsSync(fallback)) {
      res.writeHead(404, { 'content-type': 'text/plain' }).end('Client build not found');
      return;
    }
    finalPath = fallback;
  }

  const ext = finalPath.slice(finalPath.lastIndexOf('.')).toLowerCase();
  const noCache = ext === '.html' || ext === '.xml' || ext === '.txt';
  res.writeHead(200, {
    'content-type': MIME[ext] ?? 'application/octet-stream',
    'cache-control': noCache ? 'no-cache' : 'public, max-age=31536000, immutable',
  });
  if (req.method === 'HEAD') return res.end();
  res.end(readFileSync(finalPath));
}

httpServer.on('request', (req, res) => {
  const path = new URL(req.url ?? '/', 'http://localhost').pathname;
  if (path === '/health' || path === '/health/') {
    res.writeHead(200, { 'content-type': 'text/plain' }).end('ok');
    return;
  }
  if (path.startsWith('/socket.io')) return; // handled by Socket.io itself
  serveStatic(req, res);
});

httpServer.listen(PORT, () => {
  console.log(`PlusDuel server listening on :${PORT}`);
  console.log(existsSync(join(CLIENT_DIST, 'index.html'))
    ? `Serving client from ${CLIENT_DIST}`
    : `Client build not found at ${CLIENT_DIST} — run "npm run build:client" or use the dev server.`);
});
