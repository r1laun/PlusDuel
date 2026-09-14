import { Room, type Player } from './match.js';

/**
 * Matchmaking hub: FIFO queue for random duels + code-based private rooms.
 */
export class Hub {
  private queue: Player[] = [];
  private rooms = new Map<string, Room>(); // by room id
  private waitingByCode = new Map<string, WaitingRoom>();
  private socketRoom = new Map<string, string>(); // socket id -> room id

  enqueue(player: Player): void {
    this.leaveEverything(player.socket.id);
    this.queue.push(player);
    player.socket.emit('game:queued', { position: this.queue.length });
    if (this.queue.length >= 2) {
      const a = this.queue.shift()!;
      const b = this.queue.shift()!;
      this.startRoom(a, b);
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

  private startRoom(a: Player, b: Player): void {
    const room = new Room(a, b);
    this.rooms.set(room.id, room);
    this.socketRoom.set(a.socket.id, room.id);
    this.socketRoom.set(b.socket.id, room.id);
    room.start();
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
