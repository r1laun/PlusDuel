import type { LeaderboardEntry } from '@plusduel/shared';
import { Room, type Player } from './match.js';
import { RatingsStore, mergeRecords, type RatingRecord } from './ratings.js';
import { SupabaseRatingsDb, freshRecord, isSupabaseConfigured, type RatingsDb } from './supabase.js';

/**
 * Matchmaking hub: FIFO queue for random duels + code-based private rooms.
 * Quick-play matches are rated; private rooms are not.
 * Authed accounts (`acct:`) persist in Supabase; guests live in memory.
 */
export class Hub {
  private queue: Player[] = [];
  private rooms = new Map<string, Room>(); // by room id
  private waitingByCode = new Map<string, WaitingRoom>();
  private socketRoom = new Map<string, string>(); // socket id -> room id
  readonly ratings = new RatingsStore();
  private readonly db: RatingsDb | null;

  constructor(db?: RatingsDb | null) {
    this.db = db ?? (isSupabaseConfigured() ? new SupabaseRatingsDb() : null);
  }

  enqueue(player: Player): void {
    this.leaveEverything(player.socket.id);
    this.queue.push(player);
    player.socket.emit('game:queued', { position: this.queue.length });
    if (this.queue.length >= 2) {
      const a = this.queue.shift()!;
      const b = this.queue.shift()!;
      void this.startRatedRoom(a, b);
    }
  }

  dequeue(socketId: string): void {
    const idx = this.queue.findIndex((p) => p.socket.id === socketId);
    if (idx !== -1) this.queue.splice(idx, 1);
  }

  createPrivate(player: Player): string | null {
    // If host already waits on a private room, hand back its code.
    for (const w of this.waitingByCode.values()) {
      if (w.host.socket.id === player.socket.id) return w.code;
    }
    this.leaveEverything(player.socket.id);

    const room = new Room(player, player); // slot 1 is a placeholder until fill()
    const waiting = new WaitingRoom(room);
    this.waitingByCode.set(room.code!, waiting);
    this.rooms.set(room.id, room);
    this.socketRoom.set(player.socket.id, room.id);
    return room.code;
  }

  joinPrivate(player: Player, rawCode: string): boolean {
    const code = rawCode.trim().toUpperCase();
    const waiting = this.waitingByCode.get(code);
    if (!waiting || waiting.host.socket.id === player.socket.id) return false;

    this.leaveEverything(player.socket.id);
    waiting.fill(player);
    this.waitingByCode.delete(code);
    this.socketRoom.set(player.socket.id, waiting.room.id);
    waiting.room.start();
    return true;
  }

  handleDisconnect(socketId: string): void {
    this.dequeue(socketId);
    for (const [code, w] of this.waitingByCode) {
      if (w.host.socket.id === socketId) this.waitingByCode.delete(code);
    }
    const roomId = this.socketRoom.get(socketId);
    if (!roomId) return;
    const room = this.rooms.get(roomId);
    if (room) {
      room.forfeit(socketId);
      this.dropRoom(room);
    }
  }

  roomForSocket(socketId: string): Room | undefined {
    const roomId = this.socketRoom.get(socketId);
    return roomId ? this.rooms.get(roomId) : undefined;
  }

  async rankingsFor(playerId: string): Promise<{ top: LeaderboardEntry[]; you: LeaderboardEntry | null }> {
    const [dbTop, memTop] = await Promise.all([
      this.db ? this.db.top(10).catch(() => [] as LeaderboardEntry[]) : Promise.resolve([] as LeaderboardEntry[]),
      Promise.resolve(this.ratings.top(10)),
    ]);
    const top = [...dbTop, ...memTop]
      .sort((x, y) => y.rating - x.rating || x.games - y.games)
      .slice(0, 10);
    let you: LeaderboardEntry | null = null;
    if (playerId.startsWith('acct:') && this.db) {
      try {
        const rec = await this.db.load(playerId);
        if (rec && rec.games > 0) you = this.ratings.entry(rec);
      } catch {
        /* offline ladder */
      }
    } else {
      const rec = this.ratings.peek(playerId);
      if (rec && rec.games > 0) you = this.ratings.entry(rec);
    }
    return { top, you };
  }

  /**
   * Move a guest record into the signed-in account (first sign-in).
   * Returns the merged leaderboard entry, or null when there is nothing to move.
   */
  async linkAccount(userId: string, devicePlayerId: string, name: string): Promise<LeaderboardEntry | null> {
    if (!this.db) return null;
    const acctId = `acct:${userId}`;
    const guestId = devicePlayerId.trim().slice(0, 64);
    const guest = guestId && !guestId.startsWith('acct:') ? (this.ratings.peek(guestId) ?? null) : null;
    let acct: RatingRecord | null = null;
    try {
      acct = await this.db.load(acctId);
    } catch {
      return null;
    }
    const merged = mergeRecords(acct, guest, name);
    if (!merged) return null;
    merged.playerId = acctId;
    merged.name = name;
    try {
      await this.db.save(merged);
      if (guest) this.ratings.drop(guestId);
    } catch {
      return null;
    }
    return this.ratings.entry(merged);
  }

  private async startRatedRoom(a: Player, b: Player): Promise<void> {
    const [recordA, recordB] = await Promise.all([this.resolveRecord(a), this.resolveRecord(b)]);
    if (!a.socket.connected || !b.socket.connected) {
      // A side vanished while records loaded — requeue whoever is still here.
      for (const p of [a, b]) {
        if (p.socket.connected) this.enqueue(p);
      }
      return;
    }
    const room = new Room(a, b, {
      rated: true,
      recordA,
      recordB,
      onSettled: (ra, rb) => {
        void this.persist(ra, rb, a, b);
      },
    });
    this.rooms.set(room.id, room);
    this.socketRoom.set(a.socket.id, room.id);
    this.socketRoom.set(b.socket.id, room.id);
    room.start();
  }

  private async resolveRecord(p: Player): Promise<RatingRecord> {
    if (p.playerId.startsWith('acct:') && this.db) {
      try {
        const rec = await this.db.load(p.playerId);
        if (rec) {
          rec.name = p.name;
          return rec;
        }
        return freshRecord(p.playerId, p.name);
      } catch {
        /* fall through to memory */
      }
    }
    return this.ratings.for(p.playerId, p.name);
  }

  private async persist(ra: RatingRecord, rb: RatingRecord, a: Player, b: Player): Promise<void> {
    if (!this.db) return;
    try {
      await Promise.all([
        a.playerId.startsWith('acct:') ? this.db.save(ra) : Promise.resolve(),
        b.playerId.startsWith('acct:') ? this.db.save(rb) : Promise.resolve(),
      ]);
    } catch (e) {
      console.error('[ratings] persist failed:', e);
    }
  }

  private dropRoom(room: Room): void {
    this.rooms.delete(room.id);
    for (const p of room.players) {
      if (this.socketRoom.get(p.socket.id) === room.id) this.socketRoom.delete(p.socket.id);
    }
    for (const [code, w] of this.waitingByCode) {
      if (w.room === room) this.waitingByCode.delete(code);
    }
  }

  private leaveEverything(socketId: string): void {
    this.dequeue(socketId);
    for (const [code, w] of this.waitingByCode) {
      if (w.host.socket.id === socketId) this.waitingByCode.delete(code);
    }
    const roomId = this.socketRoom.get(socketId);
    if (!roomId) return;
    const room = this.rooms.get(roomId);
    if (room) {
      room.forfeit(socketId);
      this.dropRoom(room);
    }
  }
}

/** Holds a host alone until a challenger joins via room code. */
class WaitingRoom {
  constructor(readonly room: Room) {}

  get code(): string {
    return this.room.code!;
  }

  get host(): Player {
    return this.room.players[0];
  }

  fill(challenger: Player): void {
    this.room.players[1] = challenger;
  }
}
