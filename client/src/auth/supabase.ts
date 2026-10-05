import { createClient, type SupabaseClient } from '@supabase/supabase-js';

const URL = import.meta.env.VITE_SUPABASE_URL as string | undefined;
const ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined;

let client: SupabaseClient | null = null;

/** Auth is usable only when both Vite env vars are baked into the build. */
export function isAuthConfigured(): boolean {
  return !!URL && !!ANON_KEY;
}

export function supabase(): SupabaseClient | null {
  if (!isAuthConfigured()) return null;
  if (!client) {
    client = createClient(URL!, ANON_KEY!, {
      auth: { persistSession: true, autoRefreshToken: true },
    });
  }
  return client;
}

/** Fresh access token for the socket handshake (null for guests). */
export async function accessToken(): Promise<string | null> {
  try {
    const sb = supabase();
    if (!sb) return null;
    const { data } = await sb.auth.getSession();
    return data.session?.access_token ?? null;
  } catch {
    return null;
  }
}
