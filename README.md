# PlusDuel

Real-time 1v1 competitive math game. Given a target number and a multiset of digits, build an expression using **each digit exactly once** that evaluates exactly to the target. First valid answer wins the round — sudden death, best-of-5.

## Rules

- Operators: `+ − × ÷ ( ) ^ √ !` (unicode or ASCII: `- * /`)
- Digit concatenation allowed (`2`,`3` → `23`); leading zeros forbidden (`03` invalid)
- Exact rational arithmetic (fraction.js) — no floats. `√` and `!` must yield exact integers
- Digit multiset must match the issued set exactly (no reuse, no extras)

## Monorepo layout

| Path       | What                                                        |
| ---------- | ----------------------------------------------------------- |
| `shared/`  | Types, socket events, difficulty/scoring tables, validator (math.js AST + fraction.js + digit-multiset check), round generator |
| `server/`  | Socket.io server: FIFO matchmaking hub, room/match state machine, validator unit tests + E2E smoke bot |
| `client/`  | React + Vite web client: queue/join screens, round screen, hybrid input (touch tiles vs text+keyboard), live opponent status |

The validator lives in `shared/` so both tiers run identical logic:
**tier 1** — client validates instantly in the browser; **tier 2** — the server re-validates before declaring a winner (source of truth).

## Run

```bash
npm install
npm run dev:server   # Socket.io on :3001
npm run dev:client   # Vite on :5173 (proxies /socket.io to :3001)
```

## Test

```bash
npm test --workspace server    # validator + generator unit tests
npm run start --workspace server &
npm run smoke --workspace server   # two bots play a full best-of-5 match
```

## Deploy

Production is a **single Node process**: the server builds the client and serves it from the same origin, so Socket.io (websockets) needs no CORS/proxy setup.

```bash
npm install
npm run build:client   # builds client/dist
PORT=3001 npm start    # serves the app at http://host:3001
```

Requires **Node 18+**. The server reads `PORT` (set it on your platform) and `/health` returns `ok` for uptime checks.

### Platforms

**Render** (free, easiest)
1. New → Web Service → connect your repo.
2. Runtime: Node. Build command: `npm install && npm run build:client`.
3. Start command: `npm start`.
4. Render sets `PORT` automatically. Deploy. Websockets work on free tier.

**Railway / Fly.io / Heroku**
- Same two commands (`build` + `start`), same `PORT` env. One service, no extra config.

**VPS** (Ubuntu + Node)
```bash
git clone <repo> && cd PlusDuel
npm install && npm run build:client
# systemd unit: ExecStart=/usr/bin/node --import tsx node_modules/tsx/dist/cli.mjs server/src/index.ts
# (or: npm start) with Environment=PORT=3000, behind nginx → proxy_http_version 1.1 + Upgrade headers
```

### Split deploy (optional)
Serving the web build on a static host (Vercel/Netlify/Cloudflare Pages) and the Socket.io server elsewhere:
- Server side: set `CORS_ORIGIN=https://your-site.example`.
- Client side: build with `VITE_SOCKET_URL=https://your-socket-server.example` so the client connects cross-origin.

## Scoring

`score = speed_factor × difficulty_coefficient`

- `difficulty_coefficient = digit_count × ceil(target_max / 100)`
- `speed_factor = clamp(remaining_time / round_time, 0.25 … 1)` — faster answers score more; timeouts award nobody and reveal a sample solution.
