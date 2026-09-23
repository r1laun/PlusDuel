import { getStore } from '@netlify/blobs';
import type { KV } from '@plusduel/shared';

const STORE_NAME = 'plusduel';

/**
 * Netlify Blobs KV. getStore() is called per operation so it always picks up
 * the request-scoped context that Netlify injects into each invocation.
 * Strong-consistency reads so a player sees their own writes on next poll.
 */
export function blobKV(): KV {
  return {
    get: async <T>(key: string): Promise<T | null> => {
      const v = await getStore(STORE_NAME).get(key, { type: 'json', consistency: 'strong' });
      return (v ?? null) as T | null;
    },
    set: async (key: string, value: unknown): Promise<void> => {
      await getStore(STORE_NAME).setJSON(key, value);
    },
    del: async (key: string): Promise<void> => {
      await getStore(STORE_NAME).delete(key);
    },
  };
}
