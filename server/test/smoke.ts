/**
 * E2E smoke: two bots join the FIFO queue, play an entire best-of-5 match.
 * Run: npm run smoke --workspace server   (server must be running on :3001)
 */
import { io, type Socket } from 'socket.io-client';
import type { ClientToServerEvents, RoundStartPayload, ServerToClientEvents } from '@plusduel/shared';
import { findSolution } from '@plusduel/shared';

type S = Socket<ServerToClientEvents, ClientToServerEvents>;

const URL = process.env.URL ?? 'http://localhost:3001';

function bot(name: string): Promise<{ winner: string; rounds: number }> {
  return new Promise((resolve, reject) => {
    const socket: S = io(URL, { transports: ['websocket'] });
    let round = 0;
    let solved = false;
    let active = false;

    const fail = setTimeout(() => reject(new Error(`${name}: timed out`)), 120_000);

    socket.on('connect', () => socket.emit('game:queue_join', { name }));

    socket.on('round:start', (p: RoundStartPayload) => {
      active = true;
      solved = false;
      round = p.index;
      // think for a moment so we exercise real timing
      setTimeout(() => {
        if (!active || solved) return;
        const sol = findSolution(p.digits, p.target);
        if (!sol) {
          console.error(`${name}: no solution found for`, p.digits, p.target);
          return;
        }
        solved = true;
        socket.emit('round:submit', { expression: sol });
      }, 300 + Math.random() * 700);
    });

    socket.on('round:end', (p) => {
      active = false;
      console.log(
        `${name} | round ${p.index} winner=${p.winnerId === socket.id ? 'ME' : p.winnerId ?? 'nobody'} expr=${p.winningExpression ?? '-'}`,
      );
    });

    socket.on('match:start', (p) => console.log(`${name}: match vs ${p.opponent.name} (code ${p.roomCode})`));

    socket.on('match:end', (p) => {
      clearTimeout(fail);
      console.log(`${name}: MATCH END winner=${p.winnerId === socket.id ? 'ME' : 'opponent'} wins=${JSON.stringify(p.winsByPlayer)} scores=${JSON.stringify(p.scoresByPlayer)}`);
      socket.disconnect();
      resolve({ winner: p.winnerId ?? '', rounds: round });
    });

    socket.on('game:error', (p) => console.error(`${name} ERROR: ${p.message}`));
  });
}

const [a, b] = await Promise.all([bot('BotA'), bot('BotB')]);
if (!a.winner || a.winner !== b.winner) {
  console.error('SMOKE FAILED: inconsistent winners', a, b);
  process.exit(1);
}
console.log(`\nSMOKE OK — played ${a.rounds} rounds, consistent winner declared.`);
process.exit(0);
