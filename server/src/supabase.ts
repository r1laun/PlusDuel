import { createClient, type SupabaseClient } from '@supabase/supabase-js';
import { START_RATING, type LeaderboardEntry } from '@plusduel/shared';
import type { RatingRecord } from './ratings.js';

/** Minimal DB surface the Hub needs — real impl below, fakes in tests. */
export interface RatingsDb {
  load(playerId: string): Promise<RatingRecord | null>;
  save(rec: RatingRecord): Promise<void>;
  remove(playerId: string): Promise<void>;
  top(limit: number): Promise<LeaderboardEntry[]>;
}

const URL = process.env.SUPABASE_URL ?? '';
const SERVICE_KEY = process.env.SUPABASE_SERVICE_KEY ?? '';

export function isSupabaseConfigured(): boolean {
  return URL.length > 0 && SERVICE_KEY.length > 0;
}

let admin: SupabaseClient | null = null;

export function supabaseAdmin(): SupabaseClient | null {
  if (!isSupabaseConfigured()) return null;
  if (!admin) {
    admin = createClient(URL, SERVICE_KEY, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
  }
  return admin;
}

/** Verify a user's Supabase JWT (from socket handshake). Returns user id or null. */
export async function verifyToken(token: string): Promise<string | null> {
  try {
    const client = supabaseAdmin();
    if (!client) return null;
    const { data, error } = await client.auth.getUser(token);
    if (error || !data.user) return null;
    return data.user.id;
  } catch {
    return null;
  }
}

function rowToRecord(row: Record<string, any>): RatingRecord {
  return {
    playerId: row.player_id,
    name: row.name,
    rating: row.rating,
    games: row.games,
    wins: row.wins,
    losses: row.losses,
    draws: row.draws,
  };
}

/** Production RatingsDb backed by the `ratings` table (service role bypasses RLS). */
export class SupabaseRatingsDb implements RatingsDb {
  async load(playerId: string): Promise<RatingRecord | null> {
    const client = supabaseAdmin();
    if (!client) return null;
    const { data, error } = await client
      .from('ratings')
      .select('*')
      .eq('player_id', playerId)
      .maybeSingle();
    if (error || !data) return null;
    return rowToRecord(data);
  }

  async save(rec: RatingRecord): Promise<void> {
    const client = supabaseAdmin();
    if (!client) return;
    const { error } = await client.from('ratings').upsert(
      {
        player_id: rec.playerId,
        name: rec.name,
        rating: rec.rating,
        games: rec.games,
        wins: rec.wins,
        losses: rec.losses,
        draws: rec.draws,
        updated_at: new Date().toISOString(),
      },
      { onConflict: 'player_id' },
    );
    if (error) console.error('[ratings] save failed:', error.message);
  }

  async remove(playerId: string): Promise<void> {
    const client = supabaseAdmin();
    if (!client) return;
    const { error } = await client.from('ratings').delete().eq('player_id', playerId);
    if (error) console.error('[ratings] remove failed:', error.message);
  }

  async top(limit: number): Promise<LeaderboardEntry[]> {
    const client = supabaseAdmin();
    if (!client) return [];
    const { data, error } = await client
      .from('ratings')
      .select('*')
      .gt('games', 0)
      .order('rating', { ascending: false })
      .order('games', { ascending: true })
      .limit(limit);
    if (error || !data) {
      if (error) console.error('[ratings] top failed:', error.message);
      return [];
    }
    const { titleFor } = await import('@plusduel/shared');
    return data.map((row) => ({
      playerId: row.player_id,
      name: row.name,
      rating: row.rating,
      title: titleFor(row.rating),
      games: row.games,
      wins: row.wins,
      losses: row.losses,
      draws: row.draws,
    }));
  }
}

/** Fresh provisional record for ids never seen before. */
export function freshRecord(playerId: string, name: string): RatingRecord {
  return { playerId, name, rating: START_RATING, games: 0, wins: 0, losses: 0, draws: 0 };
}
