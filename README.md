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

### Option A — Render/Railway/VPS (рекомендуется для MVP)

Production представляет собой **один Node-процесс**: сервер собирает клиента и раздаёт его с того же origin, поэтому Socket.io (websockets) не требует CORS/прокси.

```bash
npm install
npm run build:client   # builds client/dist
PORT=3000 npm start    # serves the app at http://host:3000
```

Требуется **Node 18+**. Сервер читает `PORT` (устанавливается на платформе), `/health` отдаёт `ok` для uptime-чеков.

**Render** (бесплатно, проще всего)
1. New → Web Service → подключи репозиторий.
2. Runtime: Node. Build: `npm install && npm run build:client`. Start: `npm start`.
3. Render сам задаст `PORT`. Деплой. Websockets работают на free-тарифе.

### Option B — Клиент на Vercel + сервер на Render/Railway

Статика отлично ложится на Vercel, но **не** переноси на него сам Socket.io-сервер: текущие вебсокеты Vercel (Fluid) привязывают соединение к одной функции сроком до 5 мин и не гарантируют попадание игроков на один инстанс — 1v1 матчи в памяти сломаются.

**1. Запусти Socket.io-сервер на постоянном хостинге** (Render/Railway/Fly, как в Option A). Задай там:
```
CORS_ORIGIN=https://<ваш-client>.vercel.app
```

**2. Vercel (клиент)** — Import Git repo → настройки:
| Поле           | Значение                                  |
| -------------- | ----------------------------------------- |
| Root Directory | `client`                                  |
| Build          | `npm run build`                           |
| Output         | `dist` (определится автоматически)        |
| Env var        | `VITE_SOCKET_URL=https://<url-socket-сервера>` |

Env-переменная вшивается в клиент **на этапе сборки** (проверено: URL появляется в бандле). Dev-прокси в `vite.config.ts` — только для локальной разработки.

> Если Vercel не соберёт `@plusduel/shared` из workspace — замени root на весь репозиторий и укажи build: `npm install && npm run build:client`, а статикой послужит `client/dist`.

### Split deploy (запуск веб-сборки на статике в целом)
- Сервер: `CORS_ORIGIN=https://ваш-сайт`.
- Клиент: собрать с `VITE_SOCKET_URL=https://ваш-socket-сервер`.

## Scoring

`score = speed_factor × difficulty_coefficient`

- `difficulty_coefficient = digit_count × ceil(target_max / 100)`
- `speed_factor = clamp(remaining_time / round_time, 0.25 … 1)` — faster answers score more; timeouts award nobody and reveal a sample solution.
