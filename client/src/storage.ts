/**
 * localStorage that never throws (sandboxed iframes like itch.io
 * may deny storage access with a SecurityError — the game must survive that).
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
    /* storage unavailable — nickname just won't persist */
  }
}
