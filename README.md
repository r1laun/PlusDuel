<p align="center">
  <strong style="font-size: 2.2rem">Plus<span style="color:#2563eb">Duel</span></strong>
  <br/>
  <em>Real-time 1v1 competitive math</em>
</p>

<p align="center">

  <a href="https://www.typescriptlang.org"><img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-strict-3178C6?logo=typescript&logoColor=white"></a>
  <a href="https://nodejs.org"><img alt="Node" src="https://img.shields.io/badge/Node.js-22-339933?logo=nodedotjs&logoColor=white"></a>
  <a href="https://socket.io"><img alt="Socket.io" src="https://img.shields.io/badge/Realtime-Socket.io-010101?logo=socketdotio"></a>
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

Two-tier validation: the client checks instantly for zero-latency UX, the server re-validates every submission before declaring a winner. The server is the source of truth.

## Playing

- **Quick play** — FIFO matchmaking, no account required (guest nicknames on by default)
- **Private rooms** — share a room code to duel a specific friend
- **Hybrid input** — drag-and-drop tiles on touch, text + symbol keyboard on desktop

## Stack

| Layer    | Tech |
| -------- | ---- |
| Client   | React 19, Vite, socket.io-client |
| Server   | Node.js, Socket.io, in-memory room/match state machine |
| Shared   | TypeScript: exact-arithmetic validator (math.js AST), solvable round generator, difficulty/scoring |
| Testing  | Vitest (33 unit + E2E bot match), TypeScript strict |

Single-process production build: the Node server compiles and serves the web client from one origin — no proxy or CORS setup, WebSockets included.

---

<p align="center"><sub>Built with React, Socket.io, math.js and fraction.js.</sub></p>
