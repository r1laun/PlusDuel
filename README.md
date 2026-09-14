<p align="center">
  <img src="client/public/logo.png" alt="PlusDuel" width="340"/>
  <br/>
  <em>Real-time 1v1 competitive math</em>
</p>

<p align="center">

  <a href="https://www.typescriptlang.org"><img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-strict-3178C6?logo=typescript&logoColor=white"></a>
  <a href="https://nodejs.org"><img alt="Node" src="https://img.shields.io/badge/Node.js-22-339933?logo=nodedotjs&logoColor=white"></a>
  <a href="https://www.netlify.com"><img alt="Netlify" src="https://img.shields.io/badge/Hosted_on-Netlify-00C7B7?logo=netlify&logoColor=white"></a>
  <a href="https://mathjs.org"><img alt="Exact arithmetic" src="https://img.shields.io/badge/Arithmetic-exact/Fraction.js-6f2fdb?logo=mathdotcom"></a>

</p>

---

A browser multiplayer game: build a math expression equal to the target using **every issued digit exactly once**.

First valid expression wins the round. Wrong answer? You lose nothing but time — the round is sudden death.

## Gameplay

- **Target + digits** — each round deals a target number and a multiset of digits
- **Sudden death** — the first correct submission instantly wins the round; timeouts reward nobody
- **Best-of-5 match** — score reflects both speed and round difficulty
- **Learn from opponents** — after every round the winning expression is shown

## Rules are enforced, not assumed

- Operators: `+ − × ÷ ( ) ^ √ !`
- Concatenation allowed (`2 3` → `23`), leading zeros rejected (`03` invalid)
- **Exact rational math** — `fraction.js`, no floats; `√` and `!` must land on exact integers
- Every digit used exactly once; digit multiset must match the round exactly

Two-tier validation: the client checks instantly for zero-latency UX, the API re-validates every submission before declaring a winner. The server is the source of truth.

## Playing

- **Quick play** — FIFO matchmaking, no account required (guest nicknames on by default)
- **Private rooms** — share a room code to duel a specific friend
- **Hybrid input** — drag-and-drop tiles on touch, text + symbol keyboard on desktop

## Stack

| Layer    | Tech |
| -------- | ---- |
| Client   | React 19, Vite, HTTP polling (1s) |
| Server   | Netlify Functions (single `api` endpoint), Netlify Blobs for room/queue state |
| Shared   | TypeScript: exact-arithmetic validator (math.js AST), solvable round generator, difficulty/scoring, stateless match engine |
| Testing  | Vitest (45 unit + engine flow tests), TypeScript strict |

One Netlify site serves everything: the CDN hosts the static client, `/.netlify/functions/api` (redirected from `/api/*`) runs the game. Functions are stateless — round timeouts, between-round gaps and opponent forfeits resolve lazily on every poll/submit instead of via timers. Local dev: `npx netlify dev` (API on `:8888`, Vite proxies `/api` there).

---

<p align="center"><sub>Built with React, Socket.io, math.js and fraction.js.</sub></p>
