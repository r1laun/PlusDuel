/**
 * localStorage that never throws (sandboxed iframes like itch.io
  * may deny storage access with a SecurityError - the game must survive that).
 */
export function safeGet(key: string): string {
  try {
    return localStorage.getItem(key) ?? '';
  } catch {
    return '';
  }
}

export function safeSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* storage unavailable - nickname just won't persist */
  }
}

/**
 * Persistent anonymous player id for the ranked ladder.
 * Generated once per browser; the nickname stays freely changeable.
 */
export function getPlayerId(): string {
  const KEY = 'pd:playerId';
  try {
    let id = localStorage.getItem(KEY);
    if (!id) {
      id = typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID()
        : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
      localStorage.setItem(KEY, id);
    }
    return id;
  } catch {
    return '';
  }
}
